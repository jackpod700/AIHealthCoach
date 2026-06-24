import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "../stores/authStore";

import LandingView from "../views/auth/LandingView.vue";
import LoginView from "../views/auth/LoginView.vue";
import SignupView from "../views/auth/SignupView.vue";
import OAuthSuccessView from "../views/auth/OAuthSuccessView.vue";
import CalendarView from "../views/calendar/CalendarView.vue";
import ChatView from "../views/chat/ChatView.vue";
import FoodSearchView from "../views/foods/FoodSearchView.vue";
import AdminDashboardView from "../views/admin/AdminDashboardView.vue";
import ProfileView from "../views/profile/ProfileView.vue";
import DailyRecordView from "../views/records/DailyRecordView.vue";
import NotFoundView from "../views/error/NotFoundView.vue";

const appShell = {
  chat: {
    homeClass: "chat-home",
    workspaceClass: "chat-workspace",
    showTodayPanel: true,
  },
  calendar: {
    homeClass: "calendar-home",
    workspaceClass: "calendar-workspace",
    showTodayPanel: true,
  },
  records: {
    homeClass: "record-home",
    workspaceClass: "record-workspace",
    showTodayPanel: true,
  },
  foods: {
    homeClass: "food-home",
    workspaceClass: "food-workspace",
  },
  profile: {
    homeClass: "profile-home",
    workspaceClass: "profile-workspace",
    showTodayPanel: true,
  },
  admin: {
    homeClass: "admin-home",
    workspaceClass: "admin-workspace",
  },
};

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: "/",
      name: "landing",
      component: LandingView,
    },
    {
      path: "/login",
      name: "login",
      component: LoginView,
    },
    {
      path: "/signup",
      name: "signup",
      component: SignupView,
    },
    {
      path: "/oauth/success",
      name: "oauth-success",
      component: OAuthSuccessView,
    },
    {
      path: "/chat",
      name: "chat",
      component: ChatView,
      meta: {
        requiresAuth: true,
        shell: appShell.chat,
      },
    },
    {
      path: "/calendar",
      name: "calendar",
      component: CalendarView,
      meta: {
        requiresAuth: true,
        shell: appShell.calendar,
      },
    },
    {
      path: "/records",
      name: "records",
      component: DailyRecordView,
      meta: {
        requiresAuth: true,
        shell: appShell.records,
      },
    },
    {
      path: "/foods",
      name: "foods",
      component: FoodSearchView,
      meta: {
        requiresAuth: true,
        shell: appShell.foods,
      },
    },
    {
      path: "/profile",
      name: "profile",
      component: ProfileView,
      meta: {
        requiresAuth: true,
        shell: appShell.profile,
      },
    },
    {
      path: "/admin",
      name: "admin",
      component: AdminDashboardView,
      meta: {
        requiresAuth: true,
        requiresAdmin: true,
        shell: appShell.admin,
      },
    },
    {
      path: "/:pathMatch(.*)*",
      name: "not-found",
      component: NotFoundView,
    },
  ],
});

router.beforeEach((to) => {
  const authStore = useAuthStore();
  const isOAuthSignup = to.name === "signup" && to.query.oauth === "true";

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return "/login";
  }

  if (to.meta.requiresAdmin && !authStore.isAdmin) {
    return "/chat";
  }

  if (isOAuthSignup && !authStore.isAuthenticated) {
    return "/login";
  }

  if ((to.name === "login" || to.name === "signup") && authStore.isAuthenticated && !isOAuthSignup) {
    return "/chat";
  }

  return true;
});

export default router;
