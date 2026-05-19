import { createApp } from "vue";
import { createPinia } from "pinia";
import PrimeVue from "primevue/config";
import Aura from "@primeuix/themes/aura";
import Button from "primevue/button";
import Card from "primevue/card";
import Chip from "primevue/chip";
import InputText from "primevue/inputtext";
import ProgressBar from "primevue/progressbar";
import Tag from "primevue/tag";

import "primeicons/primeicons.css";
import "./styles.css";
import App from "./App.vue";

const app = createApp(App);

app.use(createPinia());
app.use(PrimeVue, {
  theme: {
    preset: Aura,
  },
});

app.component("Button", Button);
app.component("Card", Card);
app.component("Chip", Chip);
app.component("InputText", InputText);
app.component("ProgressBar", ProgressBar);
app.component("Tag", Tag);

app.mount("#app");
