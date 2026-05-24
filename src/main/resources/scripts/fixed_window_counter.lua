local base_key = KEYS[1]
local now_ms = tonumber(ARGV[1])
local window_ms = tonumber(ARGV[2])
local limit = tonumber(ARGV[3])

local window_id = math.floor(now_ms / window_ms)
local window_start_ms = window_id * window_ms
local reset_at_ms = window_start_ms + window_ms
local counter_key = base_key .. ':' .. window_id

local count = redis.call('INCR', counter_key)
redis.call('PEXPIRE', counter_key, window_ms)

local allowed = 0
if count <= limit then
  allowed = 1
end

local remaining = math.max(0, limit - count)
local retry_after_ms = 0
if allowed == 0 then
  retry_after_ms = math.max(0, reset_at_ms - now_ms)
end

return { allowed, remaining, reset_at_ms, retry_after_ms }
