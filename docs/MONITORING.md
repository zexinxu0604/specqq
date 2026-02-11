# SpecQQ Chatbot Router - Production Monitoring Guide

**Version**: 1.0.0 | **Last Updated**: 2026-02-11

This guide provides instructions for setting up observability and monitoring for the SpecQQ Chatbot Router in production environments.

---

## Table of Contents

1. [Overview](#overview)
2. [Prometheus Setup](#prometheus-setup)
3. [Grafana Dashboards](#grafana-dashboards)
4. [Key Metrics](#key-metrics)
5. [Alert Rules](#alert-rules)
6. [Log Aggregation](#log-aggregation)
7. [Health Checks](#health-checks)
8. [Performance Monitoring](#performance-monitoring)
9. [Troubleshooting](#troubleshooting)

---

## Overview

### Monitoring Stack

The SpecQQ system provides comprehensive observability through:

- **Prometheus**: Metrics collection and storage
- **Grafana**: Visualization and dashboards
- **Spring Boot Actuator**: Metrics exposure
- **Micrometer**: Metrics instrumentation
- **Logback**: Structured logging

### Architecture

```
┌─────────────────────────────────────┐
│  SpecQQ Application                 │
│  ┌───────────────────────────────┐  │
│  │ Micrometer Metrics Registry   │  │
│  │ - CQ Code Parsing Metrics     │  │
│  │ - HTTP Request Metrics        │  │
│  │ - JVM Metrics                 │  │
│  │ - Database Connection Pool    │  │
│  └───────────────┬───────────────┘  │
└──────────────────┼───────────────────┘
                   │ /actuator/prometheus
                   ▼
┌──────────────────────────────────────┐
│  Prometheus Server                   │
│  - Scrapes metrics every 15s         │
│  - Stores time-series data           │
│  - Evaluates alert rules             │
└──────────────────┬───────────────────┘
                   │ PromQL queries
                   ▼
┌──────────────────────────────────────┐
│  Grafana                             │
│  - Dashboards and visualizations     │
│  - Alerting and notifications        │
└──────────────────────────────────────┘
```

---

## Prometheus Setup

### 1. Install Prometheus

#### Using Docker

```bash
# Create Prometheus configuration directory
mkdir -p /opt/prometheus

# Create prometheus.yml (see configuration below)
nano /opt/prometheus/prometheus.yml

# Run Prometheus container
docker run -d \
  --name prometheus \
  --network host \
  -v /opt/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml \
  -v prometheus-data:/prometheus \
  prom/prometheus:latest \
  --config.file=/etc/prometheus/prometheus.yml \
  --storage.tsdb.path=/prometheus \
  --web.console.libraries=/usr/share/prometheus/console_libraries \
  --web.console.templates=/usr/share/prometheus/consoles
```

#### Using Package Manager (Ubuntu/Debian)

```bash
# Install Prometheus
sudo apt-get update
sudo apt-get install prometheus

# Edit configuration
sudo nano /etc/prometheus/prometheus.yml

# Start Prometheus service
sudo systemctl start prometheus
sudo systemctl enable prometheus
```

### 2. Prometheus Configuration

Create `/opt/prometheus/prometheus.yml`:

```yaml
# Prometheus Configuration for SpecQQ
global:
  scrape_interval: 15s      # Scrape metrics every 15 seconds
  evaluation_interval: 15s  # Evaluate rules every 15 seconds
  external_labels:
    cluster: 'specqq-production'
    environment: 'prod'

# Alertmanager configuration (optional)
alerting:
  alertmanagers:
    - static_configs:
        - targets:
            - 'localhost:9093'

# Alert rules files
rule_files:
  - '/etc/prometheus/rules/*.yml'

# Scrape configurations
scrape_configs:
  # SpecQQ Backend Application
  - job_name: 'specqq-backend'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets:
          - 'localhost:8080'
    relabel_configs:
      - source_labels: [__address__]
        target_label: instance
        replacement: 'specqq-backend-1'

  # MySQL Exporter (optional)
  - job_name: 'mysql'
    static_configs:
      - targets:
          - 'localhost:9104'

  # Redis Exporter (optional)
  - job_name: 'redis'
    static_configs:
      - targets:
          - 'localhost:9121'

  # Node Exporter (system metrics)
  - job_name: 'node'
    static_configs:
      - targets:
          - 'localhost:9100'
```

### 3. Verify Prometheus Setup

```bash
# Check Prometheus is running
curl http://localhost:9090/-/healthy

# Check targets status
curl http://localhost:9090/api/v1/targets

# Query a metric
curl -G http://localhost:9090/api/v1/query \
  --data-urlencode 'query=up{job="specqq-backend"}'
```

### 4. Access Prometheus UI

Open browser: `http://localhost:9090`

- **Status → Targets**: Verify scrape targets are UP
- **Graph**: Query and visualize metrics
- **Alerts**: View active alerts

---

## Grafana Dashboards

### 1. Install Grafana

#### Using Docker

```bash
# Run Grafana container
docker run -d \
  --name grafana \
  --network host \
  -v grafana-data:/var/lib/grafana \
  -e "GF_SECURITY_ADMIN_PASSWORD=admin" \
  grafana/grafana:latest
```

#### Using Package Manager (Ubuntu/Debian)

```bash
# Add Grafana repository
sudo apt-get install -y software-properties-common
sudo add-apt-repository "deb https://packages.grafana.com/oss/deb stable main"
wget -q -O - https://packages.grafana.com/gpg.key | sudo apt-key add -

# Install Grafana
sudo apt-get update
sudo apt-get install grafana

# Start Grafana service
sudo systemctl start grafana-server
sudo systemctl enable grafana-server
```

### 2. Configure Prometheus Data Source

1. Open Grafana: `http://localhost:3000` (default credentials: admin/admin)
2. Navigate to **Configuration → Data Sources**
3. Click **Add data source**
4. Select **Prometheus**
5. Configure:
   - Name: `Prometheus`
   - URL: `http://localhost:9090`
   - Access: `Server (default)`
6. Click **Save & Test**

### 3. Import SpecQQ Dashboard

#### Dashboard JSON

Create `specqq-dashboard.json`:

```json
{
  "dashboard": {
    "title": "SpecQQ Chatbot Router - Production Monitoring",
    "tags": ["specqq", "chatbot", "production"],
    "timezone": "browser",
    "panels": [
      {
        "id": 1,
        "title": "Request Rate (req/s)",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(http_server_requests_seconds_count{job='specqq-backend'}[5m])",
            "legendFormat": "{{method}} {{uri}}"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 0, "y": 0}
      },
      {
        "id": 2,
        "title": "Response Time P95 (ms)",
        "type": "graph",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, rate(http_server_requests_seconds_bucket{job='specqq-backend'}[5m])) * 1000",
            "legendFormat": "{{method}} {{uri}}"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 12, "y": 0}
      },
      {
        "id": 3,
        "title": "CQ Code Parse Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(cqcode_parse_total{job='specqq-backend'}[5m])",
            "legendFormat": "Parse requests/s"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 0, "y": 8}
      },
      {
        "id": 4,
        "title": "CQ Code Parse Duration P95 (ms)",
        "type": "graph",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, rate(cqcode_parse_duration_seconds_bucket{job='specqq-backend'}[5m])) * 1000",
            "legendFormat": "P95 parse time"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 12, "y": 8}
      },
      {
        "id": 5,
        "title": "Cache Hit Rate (%)",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(cqcode_cache_hits_total{job='specqq-backend'}[5m]) / (rate(cqcode_cache_hits_total{job='specqq-backend'}[5m]) + rate(cqcode_cache_misses_total{job='specqq-backend'}[5m])) * 100",
            "legendFormat": "Cache hit rate"
          }
        ],
        "yaxes": [
          {"format": "percent", "max": 100, "min": 0}
        ],
        "gridPos": {"h": 8, "w": 12, "x": 0, "y": 16}
      },
      {
        "id": 6,
        "title": "Error Rate (errors/s)",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(http_server_requests_seconds_count{job='specqq-backend',status=~'5..'}[5m])",
            "legendFormat": "{{method}} {{uri}} - {{status}}"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 12, "y": 16}
      },
      {
        "id": 7,
        "title": "JVM Memory Usage (MB)",
        "type": "graph",
        "targets": [
          {
            "expr": "jvm_memory_used_bytes{job='specqq-backend',area='heap'} / 1024 / 1024",
            "legendFormat": "Heap Used"
          },
          {
            "expr": "jvm_memory_max_bytes{job='specqq-backend',area='heap'} / 1024 / 1024",
            "legendFormat": "Heap Max"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 0, "y": 24}
      },
      {
        "id": 8,
        "title": "Database Connection Pool",
        "type": "graph",
        "targets": [
          {
            "expr": "hikaricp_connections_active{job='specqq-backend'}",
            "legendFormat": "Active"
          },
          {
            "expr": "hikaricp_connections_idle{job='specqq-backend'}",
            "legendFormat": "Idle"
          },
          {
            "expr": "hikaricp_connections{job='specqq-backend'}",
            "legendFormat": "Total"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 12, "y": 24}
      }
    ],
    "refresh": "30s",
    "time": {"from": "now-1h", "to": "now"}
  }
}
```

#### Import Dashboard

1. In Grafana, navigate to **Dashboards → Import**
2. Upload `specqq-dashboard.json` or paste JSON
3. Select Prometheus data source
4. Click **Import**

---

## Key Metrics

### Application Metrics

#### HTTP Request Metrics

```promql
# Request rate (requests per second)
rate(http_server_requests_seconds_count{job="specqq-backend"}[5m])

# P95 response time (milliseconds)
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket{job="specqq-backend"}[5m])) * 1000

# P99 response time (milliseconds)
histogram_quantile(0.99, rate(http_server_requests_seconds_bucket{job="specqq-backend"}[5m])) * 1000

# Error rate (5xx responses per second)
rate(http_server_requests_seconds_count{job="specqq-backend",status=~"5.."}[5m])

# Success rate (2xx/3xx responses)
rate(http_server_requests_seconds_count{job="specqq-backend",status=~"[23].."}[5m])
```

#### CQ Code Parsing Metrics

```promql
# Parse request rate
rate(cqcode_parse_total{job="specqq-backend"}[5m])

# P95 parse duration (milliseconds)
histogram_quantile(0.95, rate(cqcode_parse_duration_seconds_bucket{job="specqq-backend"}[5m])) * 1000

# Cache hit rate (percentage)
rate(cqcode_cache_hits_total{job="specqq-backend"}[5m]) /
(rate(cqcode_cache_hits_total{job="specqq-backend"}[5m]) + rate(cqcode_cache_misses_total{job="specqq-backend"}[5m])) * 100

# Total CQ codes parsed (gauge)
cqcode_total_count{job="specqq-backend"}
```

#### JVM Metrics

```promql
# Heap memory usage (MB)
jvm_memory_used_bytes{job="specqq-backend",area="heap"} / 1024 / 1024

# Heap memory max (MB)
jvm_memory_max_bytes{job="specqq-backend",area="heap"} / 1024 / 1024

# Heap memory utilization (percentage)
(jvm_memory_used_bytes{job="specqq-backend",area="heap"} / jvm_memory_max_bytes{job="specqq-backend",area="heap"}) * 100

# GC pause time (seconds)
rate(jvm_gc_pause_seconds_sum{job="specqq-backend"}[5m])

# Thread count
jvm_threads_live{job="specqq-backend"}
```

#### Database Metrics

```promql
# Active database connections
hikaricp_connections_active{job="specqq-backend"}

# Idle database connections
hikaricp_connections_idle{job="specqq-backend"}

# Connection pool utilization (percentage)
(hikaricp_connections_active{job="specqq-backend"} / hikaricp_connections{job="specqq-backend"}) * 100

# Connection acquisition time P95 (milliseconds)
histogram_quantile(0.95, rate(hikaricp_connections_acquire_seconds_bucket{job="specqq-backend"}[5m])) * 1000
```

---

## Alert Rules

### 1. Create Alert Rules File

Create `/etc/prometheus/rules/specqq-alerts.yml`:

```yaml
groups:
  - name: specqq_application_alerts
    interval: 30s
    rules:
      # High error rate
      - alert: HighErrorRate
        expr: |
          rate(http_server_requests_seconds_count{job="specqq-backend",status=~"5.."}[5m]) > 0.05
        for: 5m
        labels:
          severity: critical
          component: backend
        annotations:
          summary: "High error rate detected"
          description: "Error rate is {{ $value | humanize }} errors/s (threshold: 0.05)"

      # Slow response time
      - alert: SlowResponseTime
        expr: |
          histogram_quantile(0.95, rate(http_server_requests_seconds_bucket{job="specqq-backend"}[5m])) > 0.5
        for: 5m
        labels:
          severity: warning
          component: backend
        annotations:
          summary: "Slow API response time"
          description: "P95 response time is {{ $value | humanize }}s (threshold: 0.5s)"

      # Low cache hit rate
      - alert: LowCacheHitRate
        expr: |
          (rate(cqcode_cache_hits_total{job="specqq-backend"}[5m]) /
          (rate(cqcode_cache_hits_total{job="specqq-backend"}[5m]) + rate(cqcode_cache_misses_total{job="specqq-backend"}[5m]))) < 0.8
        for: 10m
        labels:
          severity: warning
          component: cache
        annotations:
          summary: "Low cache hit rate"
          description: "Cache hit rate is {{ $value | humanizePercentage }} (threshold: 80%)"

      # High memory usage
      - alert: HighMemoryUsage
        expr: |
          (jvm_memory_used_bytes{job="specqq-backend",area="heap"} / jvm_memory_max_bytes{job="specqq-backend",area="heap"}) > 0.85
        for: 5m
        labels:
          severity: warning
          component: jvm
        annotations:
          summary: "High JVM heap memory usage"
          description: "Heap usage is {{ $value | humanizePercentage }} (threshold: 85%)"

      # Database connection pool exhaustion
      - alert: DatabaseConnectionPoolExhaustion
        expr: |
          (hikaricp_connections_active{job="specqq-backend"} / hikaricp_connections{job="specqq-backend"}) > 0.9
        for: 5m
        labels:
          severity: critical
          component: database
        annotations:
          summary: "Database connection pool near exhaustion"
          description: "Connection pool utilization is {{ $value | humanizePercentage }} (threshold: 90%)"

      # Application down
      - alert: ApplicationDown
        expr: |
          up{job="specqq-backend"} == 0
        for: 1m
        labels:
          severity: critical
          component: backend
        annotations:
          summary: "SpecQQ application is down"
          description: "Application has been down for more than 1 minute"

      # Slow CQ code parsing
      - alert: SlowCQCodeParsing
        expr: |
          histogram_quantile(0.95, rate(cqcode_parse_duration_seconds_bucket{job="specqq-backend"}[5m])) > 0.01
        for: 10m
        labels:
          severity: warning
          component: cqcode
        annotations:
          summary: "Slow CQ code parsing detected"
          description: "P95 parse time is {{ $value | humanize }}s (threshold: 0.01s = 10ms)"
```

### 2. Reload Prometheus Configuration

```bash
# Send SIGHUP to reload configuration
curl -X POST http://localhost:9090/-/reload

# Or restart Prometheus
docker restart prometheus
# OR
sudo systemctl restart prometheus
```

### 3. Verify Alert Rules

```bash
# Check alert rules status
curl http://localhost:9090/api/v1/rules

# View active alerts
curl http://localhost:9090/api/v1/alerts
```

---

## Log Aggregation

### 1. Structured Logging Configuration

SpecQQ uses Logback for structured logging. Configuration is in `src/main/resources/logback-spring.xml`.

#### Log Format

```
2026-02-11 15:30:45.123 INFO  [http-nio-8080-exec-1] c.s.c.controller.CQCodeController : Parsing CQ codes from message
```

#### Log Levels

- **ERROR**: Application errors, exceptions
- **WARN**: Warnings, rate limiting triggered
- **INFO**: Important events, API calls
- **DEBUG**: Detailed debugging information (dev only)

### 2. Log File Locations

```bash
# Application logs
/var/log/specqq/application.log
/var/log/specqq/application-error.log

# Access logs
/var/log/specqq/access.log
```

### 3. Log Rotation

Configure log rotation in `/etc/logrotate.d/specqq`:

```
/var/log/specqq/*.log {
    daily
    rotate 30
    compress
    delaycompress
    missingok
    notifempty
    create 0644 specqq specqq
    postrotate
        systemctl reload specqq || true
    endscript
}
```

### 4. Centralized Logging (Optional)

#### Using ELK Stack

```bash
# Install Filebeat
curl -L -O https://artifacts.elastic.co/downloads/beats/filebeat/filebeat-8.0.0-linux-x86_64.tar.gz
tar xzvf filebeat-8.0.0-linux-x86_64.tar.gz

# Configure Filebeat
cat > filebeat.yml <<EOF
filebeat.inputs:
  - type: log
    enabled: true
    paths:
      - /var/log/specqq/*.log
    fields:
      app: specqq
      env: production

output.elasticsearch:
  hosts: ["localhost:9200"]
  index: "specqq-%{+yyyy.MM.dd}"

setup.kibana:
  host: "localhost:5601"
EOF

# Start Filebeat
./filebeat -e
```

---

## Health Checks

### 1. Application Health Endpoint

```bash
# Check overall health
curl http://localhost:8080/actuator/health

# Response (healthy):
# {
#   "status": "UP",
#   "components": {
#     "db": {"status": "UP"},
#     "redis": {"status": "UP"},
#     "diskSpace": {"status": "UP"}
#   }
# }

# Response (unhealthy):
# {
#   "status": "DOWN",
#   "components": {
#     "db": {"status": "DOWN", "details": {...}}
#   }
# }
```

### 2. Liveness and Readiness Probes

For Kubernetes deployments:

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 3

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 5
  timeoutSeconds: 3
  failureThreshold: 3
```

### 3. Custom Health Indicators

The application includes custom health indicators for:

- **Database**: MySQL connection status
- **Redis**: Redis connection and ping
- **Disk Space**: Available disk space
- **Cache**: Cache availability

---

## Performance Monitoring

### Key Performance Indicators (KPIs)

Monitor these KPIs to ensure system health:

| Metric | Target | Alert Threshold | Critical Threshold |
|--------|--------|-----------------|-------------------|
| API Response Time (P95) | <200ms | >500ms | >1000ms |
| CQ Parse Time (P95) | <10ms | >50ms | >100ms |
| Error Rate | <0.1% | >1% | >5% |
| Cache Hit Rate | >90% | <80% | <70% |
| CPU Usage | <70% | >80% | >90% |
| Memory Usage | <80% | >85% | >95% |
| Database Connections | <80% | >90% | >95% |

### Performance Baselines

Established from performance testing (T121):

- **Parse 50 CQ Codes**: P95 = 2ms (target: <10ms) ✅
- **Statistics Calculation**: P95 = 2ms (target: <50ms) ✅
- **End-to-End API**: P95 = 2ms (target: <200ms) ✅
- **Throughput**: 600+ requests/second
- **Cache Hit Rate**: ≥99.9%

---

## Troubleshooting

### High Response Time

**Symptoms**: P95 response time >500ms

**Investigation**:
```bash
# Check slow queries
curl http://localhost:8080/actuator/metrics/http.server.requests | jq '.measurements[] | select(.value > 0.5)'

# Check database connection pool
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active

# Check GC activity
curl http://localhost:8080/actuator/metrics/jvm.gc.pause
```

**Common Causes**:
- Database connection pool exhaustion
- Slow database queries
- High GC activity
- Network latency

### High Memory Usage

**Symptoms**: Heap usage >85%

**Investigation**:
```bash
# Check heap usage
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# Take heap dump
jmap -dump:live,format=b,file=heap.hprof <PID>

# Analyze with MAT (Eclipse Memory Analyzer)
```

**Common Causes**:
- Memory leaks
- Large object allocations
- Insufficient heap size
- Cache size too large

### Low Cache Hit Rate

**Symptoms**: Cache hit rate <80%

**Investigation**:
```bash
# Check cache metrics
curl http://localhost:8080/actuator/metrics/cqcode.cache.hits
curl http://localhost:8080/actuator/metrics/cqcode.cache.misses

# Check cache size
curl http://localhost:8080/actuator/metrics/cqcode.cache.size
```

**Common Causes**:
- Cache eviction due to size limits
- Diverse CQ code patterns
- Cache expiration too aggressive

---

## Monitoring Checklist

### Daily Checks

- [ ] Review Grafana dashboards for anomalies
- [ ] Check error rate in last 24 hours
- [ ] Verify all services are UP
- [ ] Review slow query logs

### Weekly Checks

- [ ] Analyze performance trends
- [ ] Review alert frequency
- [ ] Check disk space usage
- [ ] Review log aggregation pipeline

### Monthly Checks

- [ ] Review and update alert thresholds
- [ ] Capacity planning based on trends
- [ ] Update dashboards with new metrics
- [ ] Performance baseline validation

---

**Document Version**: 1.0.0
**Last Updated**: 2026-02-11
**Monitoring Stack**: Prometheus 2.x + Grafana 10.x
