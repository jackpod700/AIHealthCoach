package com.aihealthcoach.chat.experiment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;

class ContextIntentRoutingExperimentTest {

    private static final double EMBEDDING_THRESHOLD = 0.62;
    private static final int ROUTING_ITERATIONS = 200;

    private final List<IntentRoutingCase> cases = List.of(
            new IntentRoutingCase("오늘 저녁 뭐 먹을까?", ContextIntent.MEAL_ADVICE),
            new IntentRoutingCase("칼로리 맞춰서 점심 추천해줘", ContextIntent.MEAL_ADVICE),
            new IntentRoutingCase("야식 먹어도 되는지 봐줘", ContextIntent.MEAL_ADVICE),
            new IntentRoutingCase("단백질 좀 챙길 수 있는 메뉴 있을까", ContextIntent.MEAL_ADVICE),
            new IntentRoutingCase("냉장고에 닭가슴살 있는데 식단 짜줘", ContextIntent.MEAL_ADVICE),
            new IntentRoutingCase("속이 더부룩한데 가볍게 챙길만한 거", ContextIntent.MEAL_ADVICE),
            new IntentRoutingCase("유제품 피해서 도시락 추천해줘", ContextIntent.MEAL_ADVICE),
            new IntentRoutingCase("샐러드 말고 든든한 한 끼 없나", ContextIntent.MEAL_ADVICE),
            new IntentRoutingCase("운동 뭐 할까?", ContextIntent.EXERCISE_ADVICE),
            new IntentRoutingCase("오늘 헬스 루틴 잡아줘", ContextIntent.EXERCISE_ADVICE),
            new IntentRoutingCase("러닝 대신 할만한 유산소 있어?", ContextIntent.EXERCISE_ADVICE),
            new IntentRoutingCase("무릎이 별로인데 뭐 하면 돼", ContextIntent.EXERCISE_ADVICE),
            new IntentRoutingCase("허리 부담 적은 하체 루틴 알려줘", ContextIntent.EXERCISE_ADVICE),
            new IntentRoutingCase("피곤한 날에도 할 수 있는 가벼운 루틴", ContextIntent.EXERCISE_ADVICE),
            new IntentRoutingCase("요즘 활동량이 너무 적은 것 같아", ContextIntent.EXERCISE_ADVICE),
            new IntentRoutingCase("스트레칭 위주로 계획 짜줘", ContextIntent.EXERCISE_ADVICE),
            new IntentRoutingCase("요즘 왜 살이 안 빠질까?", ContextIntent.WEIGHT_TREND),
            new IntentRoutingCase("체중이 계속 그대로야", ContextIntent.WEIGHT_TREND),
            new IntentRoutingCase("몸무게 변화 추세 봐줘", ContextIntent.WEIGHT_TREND),
            new IntentRoutingCase("30일 동안 감량이 정체된 이유가 뭘까", ContextIntent.WEIGHT_TREND),
            new IntentRoutingCase("숫자가 그대로인데 식단 문제일까", ContextIntent.WEIGHT_TREND),
            new IntentRoutingCase("최근 붓기인지 살인지 모르겠어", ContextIntent.WEIGHT_TREND),
            new IntentRoutingCase("일주일 기록 기준으로 왜 더딘지 분석해줘", ContextIntent.WEIGHT_TREND),
            new IntentRoutingCase("목표까지 얼마나 남았는지 흐름 봐줘", ContextIntent.WEIGHT_TREND),
            new IntentRoutingCase("나 유제품 싫어하는 거 기억해줘", ContextIntent.MEMORY_MANAGEMENT),
            new IntentRoutingCase("앞으로 아침은 잘 못 먹는다고 저장해줘", ContextIntent.MEMORY_MANAGEMENT),
            new IntentRoutingCase("내가 무릎 부상 있는 거 메모해", ContextIntent.MEMORY_MANAGEMENT),
            new IntentRoutingCase("그 알레르기 정보 잊지 마", ContextIntent.MEMORY_MANAGEMENT),
            new IntentRoutingCase("나는 매운 음식 별로 안 좋아해", ContextIntent.MEMORY_MANAGEMENT),
            new IntentRoutingCase("앞으로 러닝보다 자전거를 선호해", ContextIntent.MEMORY_MANAGEMENT),
            new IntentRoutingCase("오늘 컨디션이 별로야", ContextIntent.GENERAL),
            new IntentRoutingCase("건강하게 생활하려면 뭐부터 바꿀까", ContextIntent.GENERAL),
            new IntentRoutingCase("내 기록 전반적으로 조언해줘", ContextIntent.GENERAL),
            new IntentRoutingCase("오늘 하루 피드백 줘", ContextIntent.GENERAL),
            new IntentRoutingCase("습관을 오래 유지하는 팁 있어?", ContextIntent.GENERAL),
            new IntentRoutingCase("최근 기록을 보고 균형 잡힌 조언 부탁해", ContextIntent.GENERAL)
    );

    @Test
    void ruleBasedRouterReturnsAUsefulFastBaseline() {
        RuleBasedContextIntentRouter router = new RuleBasedContextIntentRouter();

        ExperimentResult result = runExperiment("rule", cases, router, ROUTING_ITERATIONS);

        assertThat(result.accuracy()).isGreaterThanOrEqualTo(0.55);
        assertThat(result.averageSectionCount()).isLessThan(7.0);
    }

    @Test
    void embeddingRouterImprovesSemanticCoverageWithInMemoryPrototypeSearch() {
        RuleBasedContextIntentRouter ruleRouter = new RuleBasedContextIntentRouter();
        EmbeddingContextIntentRouter embeddingRouter = new EmbeddingContextIntentRouter(
                new InMemoryIntentPrototypeStore(new SemanticFakeEmbeddingClient()),
                new CachingEmbeddingClient(new SemanticFakeEmbeddingClient()),
                ruleRouter,
                Duration.ofMillis(200),
                EMBEDDING_THRESHOLD
        );

        ExperimentResult rule = runExperiment("rule", cases, ruleRouter, ROUTING_ITERATIONS);
        ExperimentResult embedding = runExperiment("embedding_search_only", cases, embeddingRouter, ROUTING_ITERATIONS);

        assertThat(embedding.accuracy()).isGreaterThanOrEqualTo(rule.accuracy());
        assertThat(embedding.accuracy()).isGreaterThanOrEqualTo(0.88);
        assertThat(embedding.averageContextChars()).isLessThan(2_400.0);
    }

    @Test
    void embeddingRouterFallsBackWhenEmbeddingClientFails() {
        RuleBasedContextIntentRouter ruleRouter = new RuleBasedContextIntentRouter();
        EmbeddingContextIntentRouter router = new EmbeddingContextIntentRouter(
                new InMemoryIntentPrototypeStore(new SemanticFakeEmbeddingClient()),
                message -> {
                    throw new IllegalStateException("embedding unavailable");
                },
                ruleRouter,
                Duration.ofMillis(200),
                EMBEDDING_THRESHOLD
        );

        ContextIntentRoute route = router.route("오늘 저녁 뭐 먹을까?");

        assertThat(route.intent()).isEqualTo(ContextIntent.MEAL_ADVICE);
        assertThat(route.strategy()).isEqualTo("rule_fallback");
    }

    @Test
    void cosineSimilarityHandlesSameOrthogonalAndZeroVectors() {
        assertThat(VectorMath.cosine(new double[] {1, 0}, new double[] {2, 0})).isEqualTo(1.0);
        assertThat(VectorMath.cosine(new double[] {1, 0}, new double[] {0, 1})).isEqualTo(0.0);
        assertThat(VectorMath.cosine(new double[] {0, 0}, new double[] {1, 1})).isEqualTo(0.0);
    }

    @Test
    void experimentSummaryKeepsMetricsComparableForDocumentation() {
        RuleBasedContextIntentRouter ruleRouter = new RuleBasedContextIntentRouter();
        EmbeddingContextIntentRouter embeddingRouter = new EmbeddingContextIntentRouter(
                new InMemoryIntentPrototypeStore(new SemanticFakeEmbeddingClient()),
                new CachingEmbeddingClient(new SemanticFakeEmbeddingClient()),
                ruleRouter,
                Duration.ofMillis(200),
                EMBEDDING_THRESHOLD
        );

        ExperimentResult rule = runExperiment("rule", cases, ruleRouter, ROUTING_ITERATIONS);
        ExperimentResult embedding = runExperiment("embedding_search_only", cases, embeddingRouter, ROUTING_ITERATIONS);

        String table = toMarkdownTable(List.of(rule, embedding));
        System.out.println(table);

        assertThat(table)
                .contains("strategy | cases | accuracy | p50_ms | p95_ms | avg_sections | avg_context_chars");
    }

    private ExperimentResult runExperiment(
            String strategy,
            List<IntentRoutingCase> routingCases,
            ContextIntentRouter router,
            int iterations
    ) {
        List<RouteMeasurement> measurements = new ArrayList<>();
        int correct = 0;
        int totalSections = 0;
        int totalContextChars = 0;

        for (IntentRoutingCase routingCase : routingCases) {
            ContextIntentRoute firstRoute = router.route(routingCase.message());
            if (firstRoute.intent() == routingCase.expectedIntent()) {
                correct++;
            }
            totalSections += firstRoute.sections().size();
            totalContextChars += firstRoute.estimatedContextChars();

            for (int i = 0; i < iterations; i++) {
                long startedAt = System.nanoTime();
                ContextIntentRoute route = router.route(routingCase.message());
                long elapsedNanos = System.nanoTime() - startedAt;
                measurements.add(new RouteMeasurement(route, elapsedNanos));
            }
        }

        return ExperimentResult.from(
                strategy,
                routingCases.size(),
                correct,
                measurements,
                totalSections,
                totalContextChars
        );
    }

    private String toMarkdownTable(List<ExperimentResult> results) {
        StringBuilder table = new StringBuilder();
        table.append("strategy | cases | accuracy | p50_ms | p95_ms | avg_sections | avg_context_chars\n");
        table.append("---|---:|---:|---:|---:|---:|---:\n");
        for (ExperimentResult result : results) {
            table.append(result.strategy()).append(" | ")
                    .append(result.cases()).append(" | ")
                    .append(String.format(Locale.ROOT, "%.3f", result.accuracy())).append(" | ")
                    .append(String.format(Locale.ROOT, "%.3f", result.p50Ms())).append(" | ")
                    .append(String.format(Locale.ROOT, "%.3f", result.p95Ms())).append(" | ")
                    .append(String.format(Locale.ROOT, "%.2f", result.averageSectionCount())).append(" | ")
                    .append(String.format(Locale.ROOT, "%.1f", result.averageContextChars()))
                    .append("\n");
        }
        return table.toString();
    }

    private enum ContextIntent {
        MEAL_ADVICE,
        EXERCISE_ADVICE,
        WEIGHT_TREND,
        MEMORY_MANAGEMENT,
        GENERAL
    }

    private enum ContextSection {
        PROFILE(120),
        DAILY_GOAL(80),
        TODAY_MEALS(320),
        TODAY_EXERCISES(260),
        RECENT_DAILY_SUMMARIES(560),
        RECENT_TURNS(420),
        ACTIVE_MEMORIES(260),
        FOOD_MEMORIES(180),
        EXERCISE_MEMORIES(180),
        WEIGHT_PROFILE(120);

        private final int estimatedChars;

        ContextSection(int estimatedChars) {
            this.estimatedChars = estimatedChars;
        }
    }

    private interface ContextIntentRouter {
        ContextIntentRoute route(String message);
    }

    private interface EmbeddingClient {
        double[] embed(String message);
    }

    private record IntentRoutingCase(String message, ContextIntent expectedIntent) {
    }

    private record IntentPrototype(ContextIntent intent, String text, double[] vector) {
    }

    private record ContextIntentRoute(
            ContextIntent intent,
            Set<ContextSection> sections,
            double score,
            String strategy
    ) {
        private ContextIntentRoute {
            sections = Set.copyOf(sections);
        }

        int estimatedContextChars() {
            return sections.stream()
                    .mapToInt(section -> section.estimatedChars)
                    .sum();
        }
    }

    private record RouteMeasurement(ContextIntentRoute route, long elapsedNanos) {
    }

    private record ExperimentResult(
            String strategy,
            int cases,
            double accuracy,
            double p50Ms,
            double p95Ms,
            double averageSectionCount,
            double averageContextChars
    ) {
        static ExperimentResult from(
                String strategy,
                int cases,
                int correct,
                List<RouteMeasurement> measurements,
                int totalSections,
                int totalContextChars
        ) {
            List<Long> nanos = measurements.stream()
                    .map(RouteMeasurement::elapsedNanos)
                    .sorted()
                    .toList();
            return new ExperimentResult(
                    strategy,
                    cases,
                    correct / (double) cases,
                    percentile(nanos, 0.50) / 1_000_000.0,
                    percentile(nanos, 0.95) / 1_000_000.0,
                    totalSections / (double) cases,
                    totalContextChars / (double) cases
            );
        }

        private static long percentile(List<Long> sortedValues, double percentile) {
            if (sortedValues.isEmpty()) {
                return 0L;
            }
            int index = (int) Math.ceil(percentile * sortedValues.size()) - 1;
            int boundedIndex = Math.max(0, Math.min(index, sortedValues.size() - 1));
            return sortedValues.get(boundedIndex);
        }
    }

    private static class RuleBasedContextIntentRouter implements ContextIntentRouter {

        @Override
        public ContextIntentRoute route(String message) {
            String normalized = normalize(message);

            if (containsAny(normalized, "기억", "저장", "메모", "잊지")) {
                return ContextIntentRoutingExperimentTest.route(ContextIntent.MEMORY_MANAGEMENT, 1.0, "rule");
            }
            if (containsAny(normalized, "먹", "식사", "칼로리", "저녁", "점심", "아침")) {
                return ContextIntentRoutingExperimentTest.route(ContextIntent.MEAL_ADVICE, 1.0, "rule");
            }
            if (containsAny(normalized, "운동", "헬스", "러닝", "유산소", "근력")) {
                return ContextIntentRoutingExperimentTest.route(ContextIntent.EXERCISE_ADVICE, 1.0, "rule");
            }
            if (containsAny(normalized, "살", "체중", "몸무게", "감량")) {
                return ContextIntentRoutingExperimentTest.route(ContextIntent.WEIGHT_TREND, 1.0, "rule");
            }
            return ContextIntentRoutingExperimentTest.route(ContextIntent.GENERAL, 0.0, "rule");
        }
    }

    private static class EmbeddingContextIntentRouter implements ContextIntentRouter {

        private final InMemoryIntentPrototypeStore prototypeStore;
        private final EmbeddingClient embeddingClient;
        private final ContextIntentRouter fallbackRouter;
        private final Duration timeout;
        private final double threshold;

        private EmbeddingContextIntentRouter(
                InMemoryIntentPrototypeStore prototypeStore,
                EmbeddingClient embeddingClient,
                ContextIntentRouter fallbackRouter,
                Duration timeout,
                double threshold
        ) {
            this.prototypeStore = prototypeStore;
            this.embeddingClient = embeddingClient;
            this.fallbackRouter = fallbackRouter;
            this.timeout = timeout;
            this.threshold = threshold;
        }

        @Override
        public ContextIntentRoute route(String message) {
            long startedAt = System.nanoTime();
            try {
                double[] query = embeddingClient.embed(message);
                if (elapsedSince(startedAt).compareTo(timeout) > 0) {
                    return fallback(message, "rule_timeout_fallback");
                }

                IntentPrototype best = prototypeStore.findAll().stream()
                        .max(Comparator.comparingDouble(prototype -> VectorMath.cosine(query, prototype.vector())))
                        .orElse(null);
                if (best == null) {
                    return fallback(message, "rule_empty_prototype_fallback");
                }

                double score = VectorMath.cosine(query, best.vector());
                if (score < threshold) {
                    return fallback(message, "rule_low_score_fallback");
                }

                return ContextIntentRoutingExperimentTest.route(best.intent(), score, "embedding");
            } catch (RuntimeException exception) {
                return fallback(message, "rule_fallback");
            }
        }

        private ContextIntentRoute fallback(String message, String strategy) {
            ContextIntentRoute fallback = fallbackRouter.route(message);
            return ContextIntentRoutingExperimentTest.route(fallback.intent(), fallback.score(), strategy);
        }

        private Duration elapsedSince(long startedAt) {
            return Duration.ofNanos(System.nanoTime() - startedAt);
        }
    }

    private static class InMemoryIntentPrototypeStore {

        private final List<IntentPrototype> prototypes;

        private InMemoryIntentPrototypeStore(EmbeddingClient embeddingClient) {
            this.prototypes = prototypeTexts().entrySet().stream()
                    .flatMap(entry -> entry.getValue().stream()
                            .map(text -> new IntentPrototype(entry.getKey(), text, embeddingClient.embed(text))))
                    .toList();
        }

        List<IntentPrototype> findAll() {
            return prototypes;
        }

        private Map<ContextIntent, List<String>> prototypeTexts() {
            Map<ContextIntent, List<String>> texts = new EnumMap<>(ContextIntent.class);
            texts.put(ContextIntent.MEAL_ADVICE, List.of(
                    "오늘 저녁 식단 추천",
                    "칼로리 목표에 맞는 메뉴",
                    "알레르기와 선호를 고려한 한 끼"
            ));
            texts.put(ContextIntent.EXERCISE_ADVICE, List.of(
                    "오늘 운동 루틴 추천",
                    "부상 제약을 고려한 운동",
                    "최근 활동량에 맞는 유산소 근력 계획"
            ));
            texts.put(ContextIntent.WEIGHT_TREND, List.of(
                    "최근 체중 변화 추세 분석",
                    "살이 안 빠지는 이유",
                    "식사 운동 기록 기반 감량 정체 점검"
            ));
            texts.put(ContextIntent.MEMORY_MANAGEMENT, List.of(
                    "사용자 선호를 기억해줘",
                    "알레르기 정보를 저장",
                    "앞으로 운동 취향을 메모"
            ));
            texts.put(ContextIntent.GENERAL, List.of(
                    "오늘 건강 조언",
                    "기록 전반 피드백",
                    "습관 유지 팁"
            ));
            return texts;
        }
    }

    private static class CachingEmbeddingClient implements EmbeddingClient {

        private final EmbeddingClient delegate;
        private final Map<String, double[]> cache = new ConcurrentHashMap<>();

        private CachingEmbeddingClient(EmbeddingClient delegate) {
            this.delegate = delegate;
        }

        @Override
        public double[] embed(String message) {
            return cache.computeIfAbsent(normalize(message), ignored -> delegate.embed(message));
        }
    }

    private static class SemanticFakeEmbeddingClient implements EmbeddingClient {

        private static final int DIMENSIONS = 8;
        private final Map<String, double[]> lexicon = createLexicon();

        @Override
        public double[] embed(String message) {
            String normalized = normalize(message);
            double[] vector = new double[DIMENSIONS];
            for (Map.Entry<String, double[]> entry : lexicon.entrySet()) {
                if (normalized.contains(entry.getKey())) {
                    add(vector, entry.getValue());
                }
            }
            if (VectorMath.magnitude(vector) == 0.0) {
                vector[7] = 1.0;
            }
            return VectorMath.normalize(vector);
        }

        private Map<String, double[]> createLexicon() {
            Map<String, double[]> map = new HashMap<>();
            addTerms(map, List.of("밥", "먹", "식사", "저녁", "점심", "아침", "메뉴", "식단", "칼로리", "단백질",
                    "샐러드", "야식", "도시락", "닭가슴살", "한 끼", "속이", "든든"), vector(1, 0, 0, 0, 0.2, 0, 0, 0));
            addTerms(map, List.of("운동", "헬스", "러닝", "뛰", "근력", "하체", "유산소", "스트레칭", "무릎", "허리",
                    "부상", "피곤", "루틴", "산책", "활동량", "자전거"), vector(0, 1, 0, 0, 0.2, 0.2, 0.4, 0));
            addTerms(map, List.of("살", "체중", "몸무게", "정체", "안 빠", "감량", "증량", "변화", "추세", "숫자",
                    "붓기", "더딘", "목표"), vector(0, 0, 1, 0, 0, 0.5, 0, 0));
            addTerms(map, List.of("기억", "저장", "메모", "잊지", "좋아", "싫어", "선호", "앞으로", "알레르기"),
                    vector(0.1, 0.1, 0, 1, 0, 0, 0.4, 0));
            addTerms(map, List.of("오늘", "지금", "방금"), vector(0, 0, 0, 0, 1, 0, 0, 0));
            addTerms(map, List.of("요즘", "최근", "일주일", "한달", "30일", "7일", "계속", "동안"),
                    vector(0, 0, 0.2, 0, 0, 1, 0, 0));
            addTerms(map, List.of("피해", "피하", "못", "부담", "아파", "별로", "제한", "유제품", "글루텐"),
                    vector(0.2, 0.2, 0, 0.1, 0, 0, 1, 0));
            addTerms(map, List.of("컨디션", "조언", "건강", "궁금", "생활", "전반", "피드백", "습관", "균형"),
                    vector(0, 0, 0, 0, 0.1, 0.1, 0, 1));
            return map;
        }

        private void addTerms(Map<String, double[]> map, List<String> terms, double[] vector) {
            terms.forEach(term -> map.put(term, vector));
        }

        private double[] vector(double... values) {
            return Arrays.copyOf(values, DIMENSIONS);
        }

        private void add(double[] target, double[] source) {
            for (int i = 0; i < target.length; i++) {
                target[i] += source[i];
            }
        }
    }

    private static class VectorMath {

        private static double cosine(double[] left, double[] right) {
            if (left.length != right.length) {
                throw new IllegalArgumentException("Vector dimensions must match");
            }
            double leftMagnitude = magnitude(left);
            double rightMagnitude = magnitude(right);
            if (leftMagnitude == 0.0 || rightMagnitude == 0.0) {
                return 0.0;
            }
            double dot = 0.0;
            for (int i = 0; i < left.length; i++) {
                dot += left[i] * right[i];
            }
            return dot / (leftMagnitude * rightMagnitude);
        }

        private static double magnitude(double[] vector) {
            double sum = 0.0;
            for (double value : vector) {
                sum += value * value;
            }
            return Math.sqrt(sum);
        }

        private static double[] normalize(double[] vector) {
            double magnitude = magnitude(vector);
            if (magnitude == 0.0) {
                return Arrays.copyOf(vector, vector.length);
            }
            double[] normalized = new double[vector.length];
            for (int i = 0; i < vector.length; i++) {
                normalized[i] = vector[i] / magnitude;
            }
            return normalized;
        }
    }

    private static ContextIntentRoute route(ContextIntent intent, double score, String strategy) {
        return new ContextIntentRoute(intent, sectionsFor(intent), score, strategy);
    }

    private static Set<ContextSection> sectionsFor(ContextIntent intent) {
        return switch (intent) {
            case MEAL_ADVICE -> EnumSet.of(
                    ContextSection.FOOD_MEMORIES,
                    ContextSection.DAILY_GOAL,
                    ContextSection.TODAY_MEALS,
                    ContextSection.PROFILE
            );
            case EXERCISE_ADVICE -> EnumSet.of(
                    ContextSection.EXERCISE_MEMORIES,
                    ContextSection.TODAY_EXERCISES,
                    ContextSection.DAILY_GOAL,
                    ContextSection.PROFILE
            );
            case WEIGHT_TREND -> EnumSet.of(
                    ContextSection.RECENT_DAILY_SUMMARIES,
                    ContextSection.WEIGHT_PROFILE,
                    ContextSection.DAILY_GOAL,
                    ContextSection.TODAY_MEALS,
                    ContextSection.TODAY_EXERCISES
            );
            case MEMORY_MANAGEMENT -> EnumSet.of(
                    ContextSection.ACTIVE_MEMORIES,
                    ContextSection.RECENT_TURNS
            );
            case GENERAL -> EnumSet.of(
                    ContextSection.PROFILE,
                    ContextSection.DAILY_GOAL,
                    ContextSection.RECENT_TURNS,
                    ContextSection.ACTIVE_MEMORIES
            );
        };
    }

    private static boolean containsAny(String value, String... candidates) {
        return Arrays.stream(candidates).anyMatch(value::contains);
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).trim();
    }
}
