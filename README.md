# Resilience4j Circuit Breaker Demo

Демонстрационный проект на Java 25 и Spring Boot показывает полный цикл Resilience4j Circuit Breaker 2.3.0: `CLOSED → OPEN → HALF_OPEN → CLOSED`.
В логах отображаются результат вызова, число обращений к backend, текущее состояние и метрики Circuit Breaker.
Для проверки и запуска выполните `./gradlew bootRun`
