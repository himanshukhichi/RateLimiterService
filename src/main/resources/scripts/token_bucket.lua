local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local refill_rate_per_second = tonumber(ARGV[2])
local now_ms = tonumber(ARGV[3])
local requested = tonumber(ARGV[4])

local bucket = redis.call('HMGET', key, 'tokens', 'updatedAt')
local tokens = tonumber(bucket[1])
local updated_at = tonumber(bucket[2])

if tokens == nil then
  tokens = capacity
  updated_at = now_ms
end

local elapsed_ms = math.max(0, now_ms - updated_at)
local refill = (elapsed_ms / 1000.0) * refill_rate_per_second
tokens = math.min(capacity, tokens + refill)

local allowed = 0
local retry_after_ms = 0

if tokens >= requested then
  allowed = 1
  tokens = tokens - requested
else
  retry_after_ms = math.ceil(((requested - tokens) / refill_rate_per_second) * 1000)
end

local reset_after_ms = math.ceil(((capacity - tokens) / refill_rate_per_second) * 1000)
local reset_at_ms = now_ms + reset_after_ms

redis.call('HSET', key, 'tokens', tokens, 'updatedAt', now_ms)
redis.call('PEXPIRE', key, math.max(reset_after_ms, 1000))

return { allowed, math.floor(tokens), reset_at_ms, retry_after_ms }
