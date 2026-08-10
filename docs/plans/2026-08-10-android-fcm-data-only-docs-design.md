# Android FCM data-only 검증 문서 설계

## 목표

Android 테스트 앱의 앱 내부 로그 검증에 사용할 FCM 직접 호출 예시를 data-only payload 기준으로 정리한다.

## 현재 문제

- `manual_test.md`의 Android FCM 직접 호출 예시는 notification payload를 사용한다.
- `android_noti_app`은 앱 저장소와 최근 수신 로그 UI 검증을 data-only payload 기준으로 정리했다.
- `delivery-service`의 실제 `FcmPushSender`는 현재 notification payload를 보내므로, 직접 호출 검증 경로와 서비스 경유 경로를 구분해야 한다.

## 접근

- 서버 코드는 변경하지 않는다.
- Android FCM 직접 호출 예시만 `message.data.title`, `message.data.body`를 사용하는 data-only payload로 바꾼다.
- `delivery-service` 경유 PUSH는 현재 notification payload를 보낸다고 명시한다.
- 앱 내부 로그 검증이 목적이면 직접 data-only 호출을 사용하고, provider 연동 검증이 목적이면 delivery-service 경유 경로를 사용한다고 분리한다.

## 성공 기준

- `manual_test.md`의 Android 직접 호출 JSON이 data-only payload다.
- `README.md`와 `delivery-service/DELIVERY-SERVICE-FLOW.md`가 두 검증 경로의 차이를 설명한다.
- `git diff --check`가 통과한다.
