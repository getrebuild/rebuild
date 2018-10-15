const loadDeptTree = function() {
	$.get(rb.baseUrl + '/admin/bizuser/dept-tree', function(res) {
		$('.dept-tree').empty()
		let root = $('<ul class="list-unstyled"></ul>').appendTo('.dept-tree')
		renderDeptTree({
			id : '$ALL$',
			name : '所有部门'
		}, root).addClass('active')
		$(res.data).each(function() {
			renderDeptTree(this, root)
		})
	})
}
const renderDeptTree = function(dept, target) {
	let child = $(
			'<li data-id="' + dept.id + '"><a class="text-truncate">'
					+ dept.name + '</a></li>').appendTo(target)
	child.click(function() {
		$('.dept-tree li').removeClass('active')
		child.addClass('active')
		return false
	})
	if (dept.children && dept.children.length > 0) {
		let parent = $('<ul class="list-unstyled"></ul>').appendTo(child)
		$(dept.children).each(function() {
			renderDeptTree(this, parent)
		})
	}
	return child
}