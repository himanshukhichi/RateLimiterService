#!/usr/bin/env bash
set -euo pipefail

HOST="${HOST:-127.0.0.1}"
PORT="${PORT:-8080}"
DURATION_SECONDS="${DURATION_SECONDS:-15}"
PLAN="${PLAN:-benchmarks/rate-limiter-benchmark.jmx}"
OUT_DIR="${OUT_DIR:-benchmarks/results}"

mkdir -p "$OUT_DIR"

run_case() {
  local label="$1"
  local threads="$2"
  local api_key="benchmark-${label}"
  local result_file="$OUT_DIR/${label}.jtl"

  rm -f "$result_file"
  jmeter -n \
    -t "$PLAN" \
    -JHOST="$HOST" \
    -JPORT="$PORT" \
    -JTHREADS="$threads" \
    -JDURATION_SECONDS="$DURATION_SECONDS" \
    -JAPI_KEY="$api_key" \
    -l "$result_file" >/tmp/rate-limiter-jmeter-"$label".log 2>&1

  awk -F',' -v label="$label" -v threads="$threads" '
    NR == 1 { next }
    {
      total += 1
      if ($8 == "true") success += 1
      code[$4] += 1
      elapsed += $2
      if (min_ts == 0 || $1 < min_ts) min_ts = $1
      if ($1 > max_ts) max_ts = $1
    }
    END {
      duration = (max_ts - min_ts) / 1000
      if (duration <= 0) duration = 1
      avg_latency = total == 0 ? 0 : elapsed / total
      printf "%s,%s,%d,%.2f,%.2f,%d,%d,%d\n",
        label, threads, total, total / duration, avg_latency, success, code[200], code[429]
    }
  ' "$result_file"
}

echo "target,threads,total,throughput_per_sec,avg_latency_ms,success,http_200,http_429"
run_case "10k" "${THREADS_10K:-200}"
run_case "50k" "${THREADS_50K:-400}"
run_case "100k" "${THREADS_100K:-800}"
