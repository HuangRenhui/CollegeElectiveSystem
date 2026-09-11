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
        path: 'semesters',
        name: 'AdminSemesters',
        component: () => import('@/views/admin/semesters.vue'),
        meta: { title: '学期管理', icon: 'Clock', roles: ['ADMIN'] }
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
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/404',
    meta: { hidden: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes: constantRoutes,
  scrollBehavior: () => ({ top: 0 })
})

export default router
