package tickets.metrics

import io.prometheus.metrics.core.metrics.Counter
import io.prometheus.metrics.model.registry.PrometheusRegistry
import sttp.tapir.server.metrics.prometheus.PrometheusMetrics
import sttp.tapir.server.metrics.{EndpointMetric, Metric}
object ServerMetrics {
  def register[F[_]](prometheusRegistry: PrometheusRegistry): PrometheusMetrics[F] = {
    PrometheusMetrics[F]("tapir", prometheusRegistry)
      .addCustom(
        Metric[F, Counter](
          Counter
            .builder()
            .name("requests_counter")
            .help("Total HTTP requests")
            .labelNames("path", "method", "client_type")
            .register(prometheusRegistry),
          onRequest = { (req, counter, m) =>
            m.unit {
              EndpointMetric().onEndpointRequest { ep =>
                m.eval {
                  counter.labelValues(
                    ep.showPathTemplate(),
                    req.protocol,
                    req.header("x-client-type").getOrElse("Unknown")
                  ).inc(1)
                }
              }
            }
          }
        )
      )
  }
}
