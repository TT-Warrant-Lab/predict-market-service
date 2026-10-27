# predict-market-service

Owns market, stock, trading, warrant, static-market-data, Kafka ingestion, Redis projections, and
market persistence. HTTP endpoints are service-to-service only and require `X-Internal-Key`; the
public API remains exposed through `predict-service`.

Runtime configuration:

- `APP_INTERNAL_KEY`: required inbound key shared only with `predict-service`.
- `MAS_BASE_URL`, `MAS_MARKET_ENDPOINT`, `MAS_TRADING_ENDPOINT`: direct MAS provider settings.
- Standard `SPRING_DATASOURCE_*`, `SPRING_DATA_REDIS_*`, and `SPRING_KAFKA_*` variables.

The Java namespace and application layout mirror `predict-strategy-engine`:
`com.fadx.predict_market_service` with separate `core`, `modules`, and security packages.
