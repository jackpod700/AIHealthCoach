import { createRouter, createWebHistory } from "vue-router";
import { useHealthStore } from "../stores/healthStore";
import LoginView from "../views/auth/LoginView.vue";
import SignupView from "../views/auth/SignupView.vue";
import ChatView from "../views/chat/ChatView.vue";
import ProfileView from "../views/profile/ProfileView.vue";
import NotFoundView from "../views/error/NotFoundView.vue";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: "/",
      redirect: () => {
        const healthStore = useHealthStore();
        return healthStore.isAuthenticated ? "/chat" : "/login";
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
  const healthStore = useHealthStore();

  if (to.meta.requiresAuth && !healthStore.isAuthenticated) {
    return "/login";
  }

  if ((to.name === "login" || to.name === "signup") && healthStore.isAuthenticated) {
    return "/chat";
  }

  return true;
});

export default router;
