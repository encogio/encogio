-- Receives a key, a rate limit and rate limit duration (in seconds) and returns either:
--  - {"ERROR", <ttl>} :: the rate limit has been expired, will be reset after ttl.
--  - {"OK", <remaining-attempts>} :: within limits, returns remaining attempts.
local current = tonumber(redis.call('llen', KEYS[1]));
local key = KEYS[1];
local limit = tonumber(KEYS[2]);
local duration = tonumber(KEYS[3]);

if current >= limit then
   return {'ERROR', tonumber(redis.call('ttl', key))};
else
   if tonumber(redis.call('exists', KEYS[1])) == 0 then
      redis.call('rpush', key, KEYS[1]);
      redis.call('expire', key, duration);
      return {'OK', limit - 1};
   else
      redis.call('rpushx', key, 0);
      return {'OK', limit - current - 1};
   end;
end;
