import { createRouter, createWebHistory } from 'vue-router'
import Layout from '@/layout/index.vue'

/**
 * 路由表。
 *
 * meta 字段说明：
 * - title：页面标题（用于面包屑与浏览器标题）
 * - icon：菜单图标
 * - roles：允许访问的角色数组，为空表示所有登录用户可访问
 * - hidden：是否在侧边栏隐藏
 */
export const constantRoutes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', hidden: true }
  },
  {
    path: '/404',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '页面不存在', hidden: true }
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/error/403.vue'),
    meta: { title: '无访问权限', hidden: true }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '首页', icon: 'HomeFilled' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/index.vue'),
        meta: { title: '个人中心', icon: 'User', hidden: false }
      }
    ]
  },
  // ---------------------------- 学生端 ----------------------------
  {
    path: '/student',
    component: Layout,
    redirect: '/student/courses',
    meta: { title: '学生服务', icon: 'Reading', roles: ['STUDENT'] },
    children: [
      {
        path: 'courses',
        name: 'StudentCourses',
        component: () => import('@/views/student/courses.vue'),
        meta: { title: '选课中心', icon: 'Search', roles: ['STUDENT'] }
      },
      {
        path: 'selections',
        name: 'StudentSelections',
        component: () => import('@/views/student/selections.vue'),
        meta: { title: '我的选课', icon: 'List', roles: ['STUDENT'] }
      },
      {
        path: 'timetable',
        name: 'StudentTimetable',
        component: () => import('@/views/student/timetable.vue'),
        meta: { title: '我的课表', icon: 'Calendar', roles: ['STUDENT'] }
      },
      {
        path: 'grades',
        name: 'StudentGrades',
        component: () => import('@/views/student/grades.vue'),
        meta: { title: '我的成绩', icon: 'Trophy', roles: ['STUDENT'] }
      },
      {
        path: 'reviews',
        name: 'StudentReviews',
        component: () => import('@/views/student/reviews.vue'),
        meta: { title: '教学评价', icon: 'ChatDotSquare', roles: ['STUDENT'] }
      }
    ]
  },
  // ---------------------------- 教师端 ----------------------------
  {
    path: '/teacher',
    component: Layout,
    redirect: '/teacher/workbench',
    meta: { title: '教师工作台', icon: 'Notebook', roles: ['TEACHER'] },
    children: [
      {
        path: 'workbench',
        name: 'TeacherWorkbench',
        component: () => import('@/views/teacher/workbench.vue'),
        meta: { title: '我的授课', icon: 'Grid', roles: ['TEACHER'] }
      },
      {
        path: 'timetable',
        name: 'TeacherTimetable',
        component: () => import('@/views/teacher/timetable.vue'),
        meta: { title: '教学课表', icon: 'Calendar', roles: ['TEACHER'] }
      },
      {
        path: 'reviews',
        name: 'TeacherReviews',
        component: () => import('@/views/teacher/reviews.vue'),
        meta: { title: '教学评价', icon: 'ChatDotSquare', roles: ['TEACHER'] }
      },
      {
        path: 'grades/:courseId',
        name: 'TeacherGrades',
        component: () => import('@/views/teacher/grades.vue'),
        meta: { title: '成绩录入', icon: 'EditPen', roles: ['TEACHER'], hidden: true }
      },
      {
        path: 'students/:courseId',
        name: 'TeacherStudents',
        component: () => import('@/views/teacher/students.vue'),
        meta: { title: '学生名单', icon: 'UserFilled', roles: ['TEACHER'], hidden: true }
      }
    ]
  },
  // ---------------------------- 管理端 ----------------------------
  {
    path: '/admin',
    component: Layout,
    redirect: '/admin/dashboard',
    meta: { title: '教务管理', icon: 'Setting', roles: ['ADMIN'] },
    children: [
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/dashboard.vue'),
        meta: { title: '数据概览', icon: 'DataLine', roles: ['ADMIN'] }
      },
      {
        // 选课时间（开放 / 截止）配置在本页的学期编辑弹窗里，
        // 因此提到第 2 位并改名为「学期与选课设置」，便于管理员快速找到
        path: 'semesters',
        name: 'AdminSemesters',
        component: () => import('@/views/admin/semesters.vue'),
        meta: { title: '学期与选课设置', icon: 'Timer', roles: ['ADMIN'] }
      },
      {
        path: 'courses',
        name: 'AdminCourses',
        component: () => import('@/views/admin/courses.vue'),
        meta: { title: '课程管理', icon: 'Notebook', roles: ['ADMIN'] }
      },
      {
        path: 'students',
        name: 'AdminStudents',
        component: () => import('@/views/admin/students.vue'),
        meta: { title: '学生管理', icon: 'User', roles: ['ADMIN'] }
      },
      {
        path: 'teachers',
        name: 'AdminTeachers',
        component: () => import('@/views/admin/teachers.vue'),
        meta: { title: '教师管理', icon: 'Avatar', roles: ['ADMIN'] }
      },
      {
        path: 'baseinfo',
        name: 'AdminBaseInfo',
        component: () => import('@/views/admin/baseinfo.vue'),
        meta: { title: '基础信息', icon: 'OfficeBuilding', roles: ['ADMIN'] }
      },
      {
        path: 'notices',
        name: 'AdminNotices',
        component: () => import('@/views/admin/notices.vue'),
        meta: { title: '公告管理', icon: 'Bell', roles: ['ADMIN'] }
      },
      {
        path: 'logs',
        name: 'AdminLogs',
        component: () => import('@/views/admin/logs.vue'),
        meta: { title: '操作日志', icon: 'Document', roles: ['ADMIN'] }
      },
      {
        path: 'reviews',
        name: 'AdminReviews',
        component: () => import('@/views/admin/reviews.vue'),
        meta: { title: '评价管理', icon: 'ChatDotSquare', roles: ['ADMIN'] }
      }
    ]
  },
  // ---------------------------- 公共信息（学生 / 教师） ----------------------------
  // 放在路由表末尾：侧边栏按路由顺序平铺渲染，因此它固定出现在菜单最下方。
  // 仅学生与教师可见：管理员是公告的发布方，使用「公告管理」，无需再查看此页。
  {
    path: '/info',
    component: Layout,
    redirect: '/info/notices',
    meta: { title: '公共信息', icon: 'Bell', roles: ['STUDENT', 'TEACHER'] },
    children: [
      {
        path: 'notices',
        name: 'PublicNotices',
        component: () => import('@/views/common/notices.vue'),
        meta: { title: '公共信息', icon: 'Bell', roles: ['STUDENT', 'TEACHER'] }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/404',
    meta: { hidden: true }
  }
]

/**
 * 预取全部路由组件。
 *
 * Vue Router 的导航顺序是「先加载目标页面的异步组件，再更新地址栏」，
 * 所以开发环境下第一次点击某个菜单时，页面 chunk 需要现编译，期间地址栏与
 * 内容都不会变化，看起来像「点了没反应」，等模块加载完再点一次才生效。
 * 这里在应用启动后利用浏览器空闲时间预热所有页面 chunk，让首次点击即时生效。
 */
export function prefetchRouteComponents() {
  const loaders = []

  const collect = (routes) => {
    routes.forEach((item) => {
      if (typeof item.component === 'function') {
        loaders.push(item.component)
      }
      if (Array.isArray(item.children) && item.children.length) {
        collect(item.children)
      }
    })
  }
  collect(constantRoutes)

  const run = () => {
    loaders.forEach((load) => {
      // 预取失败不影响正常使用（真正跳转时 Vue Router 还会再加载一次）
      Promise.resolve()
        .then(() => load())
        .catch(() => {})
    })
  }

  if (typeof window !== 'undefined' && typeof window.requestIdleCallback === 'function') {
    window.requestIdleCallback(run, { timeout: 3000 })
  } else {
    window.setTimeout(run, 1000)
  }
}

const router = createRouter({
  history: createWebHistory(),
  routes: constantRoutes,
  scrollBehavior: () => ({ top: 0 })
})

export default router
