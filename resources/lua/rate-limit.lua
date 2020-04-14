-- Receives a key, a rate limit and rate limit duration (in seconds) and returns either:
--  - {"ERROR", <ttl>} :: the rate limit has been expired, will be reset after ttl.
--  - {"OK", <remaining-attempts>} :: within limits, returns remaining attempts.
local key = KEYS[1];
local limit = tonumber(KEYS[2]);
local duration = tonumber(KEYS[3]);

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
