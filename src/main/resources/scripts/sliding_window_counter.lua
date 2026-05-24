local base_key = KEYS[1]
local now_ms = tonumber(ARGV[1])
local window_ms = tonumber(ARGV[2])
local limit = tonumber(ARGV[3])

local current_window_id = math.floor(now_ms / window_ms)
local previous_window_id = current_window_id - 1
local current_window_start_ms = current_window_id * window_ms
local elapsed_in_window_ms = now_ms - current_window_start_ms
local previous_weight = (window_ms - elapsed_in_window_ms) / window_ms

local current_key = base_key .. ':' .. current_window_id
local previous_key = base_key .. ':' .. previous_window_id

local current_count = tonumber(redis.call('GET', current_key) or '0')
local previous_count = tonumber(redis.call('GET', previous_key) or '0')
local estimated_count = current_count + (previous_count * previous_weight)

local allowed = 0
if estimated_count < limit then
  allowed = 1
  current_count = redis.call('INCR', current_key)
  redis.call('PEXPIRE', current_key, window_ms * 2)
  estimated_count = current_count + (previous_count * previous_weight)
else
  redis.call('PEXPIRE', current_key, window_ms * 2)
end

local reset_at_ms = current_window_start_ms + window_ms
local remaining = math.max(0, math.floor(limit - estimated_count))
local retry_after_ms = 0
if allowed == 0 then
  retry_after_ms = math.max(1, math.ceil((estimated_count - limit + 1) * (window_ms / math.max(previous_count, 1))))
end

return { allowed, remaining, reset_at_ms, retry_after_ms }
