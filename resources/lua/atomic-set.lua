-- Receives a key (KEYS[1]) and a value (ARGV[1]) and atomically sets the key to value if it doesn't exist.
--
-- Returns "OK" if successful, "duplicate key" if not.
--
local exists = tonumber(redis.call("EXISTS", KEYS[1]));
if exists == 0 then
   return redis.call("SET", KEYS[1], ARGV[1]);
else
   return redis.status_reply("duplicate key");
end;
