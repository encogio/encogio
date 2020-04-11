-- Receives a key and a value and atomically sets the key to value if it doesn't exist.
local exists = tonumber(redis.call('exists', KEYS[1]));
if exists == 0 then
  return redis.call('set', KEYS[1], KEYS[2]);
else
  return redis.status_reply('duplicate key');
end;
