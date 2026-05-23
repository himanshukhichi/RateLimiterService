local key = KEYS[1]
local now_ms = tonumber(ARGV[1])
local window_ms = tonumber(ARGV[2])
local limit = tonumber(ARGV[3])
local member = ARGV[4]
local window_start = now_ms - window_ms

redis.call('ZREMRANGEBYSCORE', key, 0, window_start)

local current_count = redis.call('ZCARD', key)
local allowed = 0
local remaining = math.max(0, limit - current_count)
local retry_after_ms = 0
local reset_at_ms = now_ms + window_ms

if current_count < limit then
  redis.call('ZADD', key, now_ms, member)
  current_count = current_count + 1
  allowed = 1
  remaining = limit - current_count
else
  local oldest = redis.call('ZRANGE', key, 0, 0, 'WITHSCORES')
  if oldest[2] ~= nil then
    reset_at_ms = tonumber(oldest[2]) + window_ms
    retry_after_ms = math.max(0, reset_at_ms - now_ms)
  else
    retry_after_ms = window_ms
  end
end

redis.call('PEXPIRE', key, window_ms)

return { allowed, remaining, reset_at_ms, retry_after_ms }
