/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.business.approval;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.RebuildException;
import com.rebuild.server.configuration.FlowDefinition;
import com.rebuild.server.configuration.RobotApprovalManager;
import com.rebuild.server.helper.ConfigurationException;
import com.rebuild.server.helper.SetUser;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.EntityService;
import com.rebuild.server.service.base.ApprovalStepService;
import com.rebuild.server.service.bizz.UserHelper;
import com.rebuild.utils.JSONUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 审批处理
 * 
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/06/24
 */
public class ApprovalProcessor extends SetUser<ApprovalProcessor> {

	private  static final Log LOG = LogFactory.getLog(ApprovalProcessor.class);

	// 最大撤销次数
	private static final int MAX_REVOKED = 3;

	final private ID record;

	// 如未传递，会在需要时根据 record 确定
	private ID approval;
	// 流程定义
	private FlowParser flowParser;
	
	/**
	 * @param record
	 */
	public ApprovalProcessor(ID record) {
		this(record, null);
	}
	
	/**
	 * @param record
	 * @param approval
	 */
	public ApprovalProcessor(ID record, ID approval) {
		this.record = record;
		this.approval = approval;
	}
	
	/**
	 * 提交
	 * 
	 * @param selectNextUsers
	 * @return
	 * @throws ApprovalException
	 */
	public boolean submit(JSONObject selectNextUsers) throws ApprovalException {
		final ApprovalState currentState = ApprovalHelper.getApprovalState(this.record);
		if (currentState == ApprovalState.PROCESSING || currentState == ApprovalState.APPROVED) {
			throw new ApprovalException("当前记录已经" + (currentState == ApprovalState.PROCESSING ? "提交审批" : "审批完成"));
		}
		
		FlowNodeGroup nextNodes = getNextNodes(FlowNode.NODE_ROOT);
		if (!nextNodes.isValid()) {
			LOG.warn("No next-node be found");
			return false;
		}

		Set<ID> nextApprovers = nextNodes.getApproveUsers(this.getUser(), this.record, selectNextUsers);
		if (nextApprovers.isEmpty()) {
			LOG.warn("No any approvers special");
			return false;
		}

		Set<ID> ccs = nextNodes.getCcUsers(this.getUser(), this.record, selectNextUsers);
		Set<ID> ccs4share = nextNodes.getCcUsers4Share(this.getUser(), this.record, selectNextUsers);

		Record mainRecord = EntityHelper.forUpdate(this.record, this.getUser(), false);
		mainRecord.setID(EntityHelper.ApprovalId, this.approval);
		mainRecord.setInt(EntityHelper.ApprovalState, ApprovalState.PROCESSING.getState());
		mainRecord.setString(EntityHelper.ApprovalStepNode, nextNodes.getApprovalNode().getNodeId());
		Application.getBean(ApprovalStepService.class).txSubmit(mainRecord, ccs, nextApprovers);

		// 非主事物
		shareIfNeed(this.record, ccs4share);

		return true;
	}

	/**
	 * 审批
	 *
	 * @param approver
	 * @param state
	 * @param remark
	 * @param selectNextUsers
	 * @throws ApprovalException
	 */
	public void approve(ID approver, ApprovalState state, String remark, JSONObject selectNextUsers) throws ApprovalException {
		approve(approver, state, remark, selectNextUsers, null);
	}

	/**
	 * 审批
	 *
	 * @param approver
	 * @param state
	 * @param remark
	 * @param selectNextUsers
	 * @param addedData
	 * @throws ApprovalException
	 */
	public void approve(ID approver, ApprovalState state, String remark, JSONObject selectNextUsers, Record addedData) throws ApprovalException {
		final ApprovalStatus status = ApprovalHelper.getApprovalStatus(this.record);
		ApprovalState currentState = status.getCurrentState();
		if (currentState != ApprovalState.PROCESSING) {
			throw new ApprovalException("当前记录审批已" + currentState.getName());
		}

		final Object[] stepApprover = Application.createQueryNoFilter(
				"select stepId,state,node,approvalId from RobotApprovalStep where recordId = ? and approver = ? and node = ? and isCanceled = 'F'")
				.setParameter(1, this.record)
				.setParameter(2, approver)
				.setParameter(3, getCurrentNodeId(status))
				.unique();
		if (stepApprover == null || (Integer) stepApprover[1] != ApprovalState.DRAFT.getState()) {
			throw new ApprovalException(stepApprover == null ? "当前流程已经被他人审批" : "你已经审批过当前流程");
		}
		
		Record approvedStep = EntityHelper.forUpdate((ID) stepApprover[0], approver);
		approvedStep.setInt("state", state.getState());
		approvedStep.setDate("approvedTime", CalendarUtils.now());
		if (StringUtils.isNotBlank(remark)) {
			approvedStep.setString("remark", remark);
		}
		
		this.approval = (ID) stepApprover[3];
		FlowNodeGroup nextNodes = getNextNodes((String) stepApprover[2]);

		Set<ID> nextApprovers = null;
		String nextNode = null;

		if (state == ApprovalState.APPROVED && !nextNodes.isLastStep()) {
			nextApprovers = nextNodes.getApproveUsers(this.getUser(), this.record, selectNextUsers);
			if (nextApprovers.isEmpty()) {
				throw new ApprovalException("无下一步审批人可用，请联系管理员配置");
			}

			FlowNode nextApprovalNode = nextNodes.getApprovalNode();
			nextNode = nextApprovalNode != null ? nextApprovalNode.getNodeId() : null;
		}

		Set<ID> ccs = nextNodes.getCcUsers(this.getUser(), this.record, selectNextUsers);
		Set<ID> ccs4share = nextNodes.getCcUsers4Share(this.getUser(), this.record, selectNextUsers);

		FlowNode currentNode = getFlowParser().getNode((String) stepApprover[2]);
		Application.getBean(ApprovalStepService.class)
				.txApprove(approvedStep, currentNode.getSignMode(), ccs, nextApprovers, nextNode, addedData);

		// 非主事物
		shareIfNeed(this.record, ccs4share);
	}

	/**
	 * 撤回
	 *
	 * @throws ApprovalException
	 */
	public void cancel() throws ApprovalException {
		final ApprovalStatus status = ApprovalHelper.getApprovalStatus(this.record);
		ApprovalState currentState = status.getCurrentState();
		if (currentState != ApprovalState.PROCESSING) {
			throw new ApprovalException("已" + currentState.getName() + "审批不能撤回");
		}

		Application.getBean(ApprovalStepService.class).txCancel(
				this.record, status.getApprovalId(), getCurrentNodeId(status), false);
	}

	/**
	 * 撤回
	 *
	 * @throws ApprovalException
	 */
	public void revoke() throws ApprovalException {
		final ApprovalStatus status = ApprovalHelper.getApprovalStatus(this.record);
		if (status.getCurrentState() != ApprovalState.APPROVED) {
			throw new ApprovalException("未完成审批无需撤销");
		}

		Object[] count = Application.createQueryNoFilter(
				"select count(stepId) from RobotApprovalStep where recordId = ? and state = ?")
				.setParameter(1, this.record)
				.setParameter(2, ApprovalState.REVOKED.getState())
				.unique();
		if (ObjectUtils.toInt(count[0]) >= MAX_REVOKED) {
			throw new ApprovalException("记录撤销次数已达 " + MAX_REVOKED + " 次，不能再次撤销");
		}

		Application.getBean(ApprovalStepService.class).txCancel(
				this.record, status.getApprovalId(), getCurrentNodeId(status), true);
	}

	/**
	 * @return
	 */
	public FlowNode getCurrentNode() {
		return getFlowParser().getNode(getCurrentNodeId(null));
	}

	/**
	 * @return
	 * @see #getNextNode(String)
	 */
	protected FlowNode getNextNode() {
		return getNextNode(getCurrentNodeId(null));
	}
	
	/**
	 * 获取下一节点
	 * 
	 * @param currentNode
	 * @return
	 */
	protected FlowNode getNextNode(String currentNode) {
		Assert.notNull(currentNode, "[currentNode] not be null");
		
		List<FlowNode> nextNodes = getFlowParser().getNextNodes(currentNode);
		if (nextNodes.isEmpty()) {
			return null;
		}
		
		FlowNode firstNode = nextNodes.get(0);
		if (!FlowNode.TYPE_BRANCH.equals(firstNode.getType())) {
			return firstNode;
		}
		
		int bLength = nextNodes.size();
		for (FlowNode node : nextNodes) {
			// 匹配最后一个
			if (--bLength == 0) {
				return getNextNode(node.getNodeId());
			}
			
			FlowBranch branch = (FlowBranch) node;
			if (branch.matches(record)) {
				return getNextNode(branch.getNodeId());
			}
		}
		return null;
	}
	
	/**
	 * @return
	 * @see #getNextNodes(String)
	 */
	public FlowNodeGroup getNextNodes() {
		return getNextNodes(getCurrentNodeId(null));
	}
	
	/**
	 * 获取下一组节点。遇到审批人节点则终止，在审批节点前有抄送节点也会返回
	 * 
	 * @param currentNode
	 * @return
	 */
	protected FlowNodeGroup getNextNodes(String currentNode) {
		Assert.notNull(currentNode, "[currentNode] not be null");
		
		FlowNodeGroup nodes = new FlowNodeGroup();
		FlowNode next = null;
		while (true) {
			next = getNextNode(next != null ? next.getNodeId() : currentNode);
			if (next == null) {
				break;
			}
			
			nodes.addNode(next);
			if (FlowNode.TYPE_APPROVER.equals(next.getType())) {
				break;
			}
		}
		return nodes;
	}

	/**
	 * 获取当前审批节点 ID
	 *
	 * @param useStatus
	 * @return
	 */
	private String getCurrentNodeId(ApprovalStatus useStatus) {
		if (useStatus == null) useStatus = ApprovalHelper.getApprovalStatus(this.record);

		String currentNode = useStatus.getCurrentStepNode();
		if (StringUtils.isBlank(currentNode)
				|| useStatus.getCurrentState().getState() >= ApprovalState.REJECTED.getState()) {
			currentNode = FlowNode.NODE_ROOT;
		}
		return currentNode;
	}
	
	/**
	 * @return
	 */
	private FlowParser getFlowParser() {
		Assert.notNull(approval, "[approval] not be null");
		if (flowParser != null) {
			return flowParser;
		}
		
		FlowDefinition flowDefinition = RobotApprovalManager.instance.getFlowDefinition(
				MetadataHelper.getEntity(this.record.getEntityCode()), this.approval);
		flowParser = flowDefinition.createFlowParser();
		return flowParser;
	}

	/**
	 * 获取当前审批步骤
	 * 
	 * @return returns [S, S]
	 */
	public JSONArray getCurrentStep() {
		final String currentNode = ApprovalHelper.getApprovalStatus(this.record).getCurrentStepNode();
		Object[][] array = Application.createQueryNoFilter(
				"select approver,state,remark,approvedTime,createdOn from RobotApprovalStep"
				+ " where recordId = ? and approvalId = ? and node = ? and isCanceled = 'F'")
				.setParameter(1, this.record)
				.setParameter(2, this.approval)
				.setParameter(3, currentNode)
				.array();

		JSONArray steps = new JSONArray();
		for (Object[] o : array) {
			steps.add(this.formatStep(o, null));
		}
		return steps;
	}
	
	/**
	 * 获取已执行流程列表
	 * 
	 * @return returns [ [S,S], [S], [SSS], [S] ]
	 */
	public JSONArray getWorkedSteps() {
		final ApprovalStatus status = ApprovalHelper.getApprovalStatus(this.record);
		this.approval = status.getApprovalId();

		Object[][] array = Application.createQueryNoFilter(
				"select approver,state,remark,approvedTime,createdOn,createdBy,node,prevNode from RobotApprovalStep" +
				" where recordId = ? and isWaiting = 'F' and isCanceled = 'F' order by createdOn")
				.setParameter(1, this.record)
				.array();
		if (array.length == 0) {
			return JSONUtils.EMPTY_ARRAY;
		}

		Object[] firstStep = null;
		Map<String, List<Object[]>> stepGroupMap = new HashMap<>();
		for (Object[] o : array) {
			String prevNode = (String) o[7];
			if (firstStep == null && FlowNode.NODE_ROOT.equals(prevNode)) {
				firstStep = o;
			}

			List<Object[]> stepGroup = stepGroupMap.computeIfAbsent(prevNode, k -> new ArrayList<>());
			stepGroup.add(o);
		}
		if (firstStep == null) {
			throw new RebuildException("无效审批记录 : " + this.record);
		}

		JSONArray steps = new JSONArray();
		JSONObject submitter = JSONUtils.toJSONObject(
				new String[] { "submitter", "submitterName", "createdOn", "approvalId", "approvalName", "approvalState" },
				new Object[] { firstStep[5],
						UserHelper.getName((ID) firstStep[5]),
						CalendarUtils.getUTCDateTimeFormat().format(firstStep[4]),
						status.getApprovalId(), status.getApprovalName(), status.getCurrentState().getState() });
		steps.add(submitter);

		String next = FlowNode.NODE_ROOT;
		while (next != null) {
			List<Object[]> group = stepGroupMap.get(next);
			if (group == null) {
				break;
			}
			next = (String) group.get(0)[6];

			// 按审批时间排序
			group.sort((o1, o2) -> {
				Date t1 = (Date) (o1[3] == null ? o1[4] : o1[3]);
				Date t2 = (Date) (o2[3] == null ? o2[4] : o2[3]);
				return t1.compareTo(t2);
			});

			String signMode = null;
			try {
				signMode = getFlowParser().getNode(next).getSignMode();
			} catch (ApprovalException | ConfigurationException ignored) {
			}

			JSONArray s = new JSONArray();
			for (Object[] o : group) {
				s.add(formatStep(o, signMode));
			}
			steps.add(s);
		}
		return steps;
	}

	/**
	 * @param step
	 * @param signMode
	 * @return
	 */
	private JSONObject formatStep(Object[] step, String signMode) {
		ID approver = (ID) step[0];
		return JSONUtils.toJSONObject(
				new String[] { "approver", "approverName", "state", "remark", "approvedTime", "createdOn", "signMode" },
				new Object[] {
						approver, UserHelper.getName(approver),
						step[1], step[2],
						step[3] == null ? null: CalendarUtils.getUTCDateTimeFormat().format(step[3]),
						CalendarUtils.getUTCDateTimeFormat().format(step[4]), signMode });
	}

	/**
	 * CC 自动共享
	 *
	 * @param recordId
	 * @param shareTo
	 * @return
	 */
	private int shareIfNeed(ID recordId, Set<ID> shareTo) {
		final EntityService es = Application.getEntityService(recordId.getEntityCode());

		int shared = 0;
		for (ID user : shareTo) {
			if (!Application.getPrivilegesManager().allowRead(user, recordId)) {
				es.share(recordId, user, null);
				shared++;
			}
		}
		return shared;
	}
}
