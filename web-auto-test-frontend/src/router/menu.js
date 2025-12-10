const menuConfig = [
  { title: "首页", name: "Home", path: "/home", icon: "HomeFilled" },
  {
    title: "用例管理",
    name: "CaseManage",
    path: "/case-manage",
    icon: "Files",
  },
  {
    title: "元素管理",
    name: "ElementManage",
    path: "/element-manage",
    icon: "Monitor",
  },
  {
    title: "任务调度",
    name: "TaskSchedule",
    path: "/task-schedule",
    icon: "Timer",
  },
  { title: "测试报告", name: "Report", path: "/report", icon: "PieChart" },
  {
    title: "系统管理",
    name: "System",
    path: "/system",
    icon: "Setting",
    requiresAdmin: true,
  },
];

export default menuConfig;
