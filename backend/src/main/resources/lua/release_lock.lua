--[[
  仅当锁值匹配时删除，避免误删其它线程持有的短锁
  KEYS[1] = lock key
  ARGV[1] = lock token
  返回：1 已删除 / 0 非持有者或锁已过期
]]
if redis.call('GET', KEYS[1]) == ARGV[1] then
    return redis.call('DEL', KEYS[1])
end
return 0
