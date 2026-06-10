import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "../stores/authStore";
import LoginView from "../views/auth/LoginView.vue";
import SignupView from "../views/auth/SignupView.vue";
import CalendarView from "../views/calendar/CalendarView.vue";
import ChatView from "../views/chat/ChatView.vue";
import FoodSearchView from "../views/foods/FoodSearchView.vue";
import ProfileView from "../views/profile/ProfileView.vue";
import DailyRecordView from "../views/records/DailyRecordView.vue";
import NotFoundView from "../views/error/NotFoundView.vue";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: "/",
      redirect: () => {
        const authStore = useAuthStore();
        return authStore.isAuthenticated ? "/chat" : "/login";
      },
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
      path: "/chat",
      name: "chat",
      component: ChatView,
      meta: {
        requiresAuth: true,
      },
    },
    {
      path: "/calendar",
      name: "calendar",
      component: CalendarView,
      meta: {
        requiresAuth: true,
      },
    },
    {
      path: "/records",
      name: "records",
      component: DailyRecordView,
      meta: {
        requiresAuth: true,
      },
    },
    {
      path: "/foods",
      name: "foods",
      component: FoodSearchView,
      meta: {
        requiresAuth: true,
      },
    },
    {
      path: "/profile",
      name: "profile",
      component: ProfileView,
      meta: {
        requiresAuth: true,
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

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return "/login";
  }

  if ((to.name === "login" || to.name === "signup") && authStore.isAuthenticated) {
    return "/chat";
  }

  return true;
});

export default router;
