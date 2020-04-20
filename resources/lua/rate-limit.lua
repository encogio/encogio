-- Receives a key (KEYS[1]), a rate limit (ARGV[1]) and rate limit duration in seconds (ARGV[2]) and tries to increase the rate limit count for key given the configuration.
--
-- Returns either:
--  {"ERROR", <ttl>}
--    The rate limit has been expired, will be reset after `ttl` seconds.
--
--  {"OK", <remaining-attempts>}
--     Within rate limit threshold, returns remaining attempts.
--
local key = KEYS[1];
local limit = tonumber(ARGV[1]);
local duration = tonumber(ARGV[2]);

local current = tonumber(redis.call("GET", key));
if current ~= nil and current >= limit then
   return {"ERROR", tonumber(redis.call("TTL", key))};
else
   local counter = redis.call("INCR", key)
   if counter == 1 then
      redis.call("EXPIRE", key, duration);
   end;
   return {"OK", limit - counter};
end;
