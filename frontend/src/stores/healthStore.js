import { defineStore } from "pinia";

export const useHealthStore = defineStore("health", {
  state: () => ({
    quickPrompts: [
      "음식 기록하기",
      "운동 기록하기",
      "물 마신 양 기록하기",
      "오늘 부족한 것 물어보기",
    ],
    messages: [
      {
        id: 1,
        role: "bot",
        text: "안녕하세요. 먹은 음식, 운동, 수분을 자연스럽게 말해주세요.",
      },
      {
        id: 2,
        role: "user",
        text: "아침에 그릭요거트랑 블루베리 먹고 30분 산책했어.",
      },
      {
        id: 3,
        role: "bot",
        text: "아침 식사와 산책 기록 후보를 만들었어요. 오른쪽에서 확인해보세요.",
      },
    ],
    records: [
      {
        id: 1,
        type: "식단",
        severity: "info",
        time: "08:00",
        title: "아침 식사",
        description: "그릭요거트, 블루베리, 견과류",
        value: "342 kcal",
      },
      {
        id: 2,
        type: "운동",
        severity: "success",
        time: "09:10",
        title: "가벼운 산책",
        description: "중간 강도 산책 30분",
        value: "120 kcal 소모",
      },
      {
        id: 3,
        type: "수분",
        severity: "warn",
        time: "10:30",
        title: "물 섭취",
        description: "오전 수분 기록",
        value: "500 ml",
      },
    ],
    summary: {
      score: 87,
      calories: 1742,
      calorieGoal: 2000,
      protein: 112,
      proteinGoal: 122,
      water: 1.3,
      waterGoal: 2,
    },
  }),
  actions: {
    addMessage(text) {
      this.messages.push({
        id: Date.now(),
        role: "user",
        text,
      });
      this.messages.push({
        id: Date.now() + 1,
        role: "bot",
        text: "입력 내용을 분석해 기록 후보로 정리할게요.",
      });
    },
  },
});
