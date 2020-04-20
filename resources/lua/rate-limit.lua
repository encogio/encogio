-- Receives a key (KEYS[1]), a rate limit (ARGV[1]) and rate limit duration in seconds (ARGV[2]) and returns either:
--  - {"ERROR", <ttl>} :: the rate limit has been expired, will be reset after ttl.
--  - {"OK", <remaining-attempts>} :: within limits, returns remaining attempts.
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
