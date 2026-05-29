INSERT INTO users (id, email, password, nickname, created_at, updated_at)
VALUES
    (1, 'test@example.com', '$2a$10$AAxwQvsqyN903xxJSKqC.eFOdhMKNf2xtiQYDbt/RdD6Q3a.qa6Oq', '테스트유저', NOW(), NOW()),
    (2, 'runner@example.com', '$2a$10$AAxwQvsqyN903xxJSKqC.eFOdhMKNf2xtiQYDbt/RdD6Q3a.qa6Oq', '러닝초보', NOW(), NOW()),
    (3, 'diet@example.com', '$2a$10$AAxwQvsqyN903xxJSKqC.eFOdhMKNf2xtiQYDbt/RdD6Q3a.qa6Oq', '다이어터', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

INSERT INTO user_profiles (id, user_id, height_cm, current_weight_kg, target_weight_kg, goal_type, updated_at)
VALUES
    (1, 1, 172.50, 68.40, 65.00, 'MAINTAIN', NOW()),
    (2, 2, 178.20, 82.10, 76.00, 'MUSCLE_GAIN', NOW()),
    (3, 3, 164.00, 59.30, 55.00, 'WEIGHT_LOSS', NOW())
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO chat_messages (id, user_id, role, content, created_at)
SELECT row_number() OVER () AS id, user_id, role, content, created_at
FROM (VALUES
    (1, 'USER', '아침에 그릭요거트랑 블루베리 먹었어.', NOW() - INTERVAL '85 seconds'),
    (1, 'ASSISTANT', '아침 식사로 기록할게요. 예상 칼로리는 약 342kcal입니다.', NOW() - INTERVAL '80 seconds'),
    (1, 'USER', '견과류도 한 줌 정도 같이 먹었어.', NOW() - INTERVAL '75 seconds'),
    (1, 'ASSISTANT', '견과류를 아침 식사에 추가했어요. 지방과 단백질 섭취가 조금 더 보완됐습니다.', NOW() - INTERVAL '70 seconds'),
    (1, 'USER', '점심에는 닭가슴살 샐러드랑 현미밥 반 공기 먹었어.', NOW() - INTERVAL '65 seconds'),
    (1, 'ASSISTANT', '점심 식사로 닭가슴살 샐러드와 현미밥을 기록했어요. 단백질 섭취가 좋아요.', NOW() - INTERVAL '60 seconds'),
    (1, 'USER', '오후에 물 500ml 마셨어.', NOW() - INTERVAL '55 seconds'),
    (1, 'ASSISTANT', '수분 500ml를 기록했어요. 오늘 목표까지 약 700ml 정도 남았습니다.', NOW() - INTERVAL '50 seconds'),
    (1, 'USER', '퇴근하고 30분 정도 빠르게 걸었어.', NOW() - INTERVAL '45 seconds'),
    (1, 'ASSISTANT', '빠른 걷기 30분을 운동 기록으로 저장했어요. 예상 소모 칼로리는 약 140kcal입니다.', NOW() - INTERVAL '40 seconds'),
    (1, 'USER', '저녁은 김치찌개랑 밥 한 공기 먹었는데 괜찮을까?', NOW() - INTERVAL '35 seconds'),
    (1, 'ASSISTANT', '김치찌개는 나트륨이 높을 수 있어요. 오늘은 물을 조금 더 마시고 야식은 가볍게 조절해보세요.', NOW() - INTERVAL '30 seconds'),
    (1, 'USER', '오늘 단백질은 충분해?', NOW() - INTERVAL '25 seconds'),
    (1, 'ASSISTANT', '현재 기록 기준 단백질은 목표의 약 90% 수준이에요. 부족하면 삶은 달걀이나 두부를 조금 추가해도 좋습니다.', NOW() - INTERVAL '20 seconds'),
    (1, 'USER', '내일은 어떤 운동 하면 좋을까?', NOW() - INTERVAL '15 seconds'),
    (1, 'ASSISTANT', '오늘 걷기를 했으니 내일은 하체 근력 운동 20분과 가벼운 스트레칭을 추천합니다.', NOW() - INTERVAL '10 seconds'),
    (1, 'USER', '오늘 요약 보여줘.', NOW() - INTERVAL '5 seconds'),
    (1, 'ASSISTANT', '오늘은 식단 기록이 안정적이고 활동량도 좋아요. 다만 수분 섭취가 조금 부족하니 물 한 잔을 더 마셔보세요.', NOW())
) AS seed(user_id, role, content, created_at)
ON CONFLICT (id) DO NOTHING;

SELECT setval('users_id_seq', COALESCE((SELECT MAX(id) FROM users), 1));
SELECT setval('user_profiles_id_seq', COALESCE((SELECT MAX(id) FROM user_profiles), 1));
SELECT setval('chat_messages_id_seq', COALESCE((SELECT MAX(id) FROM chat_messages), 1));
