package com.rebuild.core.service.sop;

import com.rebuild.core.service.general.OperatingContext;
import com.rebuild.core.service.general.OperatingObserver;
import com.rebuild.rbv.sop.RobotSopManager;
import com.rebuild.rbv.sop.StepNode;
import lombok.extern.slf4j.Slf4j;

/**
 * Observer for SOP
 *
 * @author devezhao
 * @since 2024/4/12
 */
@Slf4j
public class RobotSopObserver extends OperatingObserver  {

    @Override
    public int getOrder() {
        return 5;
    }

    @Override
    protected void onCreate(OperatingContext context) {
        this.onCreateOrUpdate(context);
    }

    @Override
    protected void onUpdate(OperatingContext context) {
        this.onCreateOrUpdate(context);
    }

    /**
     * @param context
     */
    protected void onCreateOrUpdate(OperatingContext context) {
        StepNode[] stepNodes = RobotSopManager.instance.findSteps(context.getAnyRecord().getEntity());

        for (StepNode stepNode : stepNodes) {
            stepNode.recordIfAchieved(context);
        }
    }
}
