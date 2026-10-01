--[[
  原子重建课程容量与已选集合。持锁期间 select/drop 脚本会拒绝执行。
  KEYS[1] = capacity key
  KEYS[2] = selected set key
  KEYS[3] = rebuild lock key
  ARGV[1] = lock token
  ARGV[2] = remaining capacity
  ARGV[3] = ttl seconds
  ARGV[4..] = student ids（并集：数据库有效选课 ∪ Redis 中尚未落库的预占）
  返回：1 成功 / 0 非锁持有者
]]
if redis.call('GET', KEYS[3]) ~= ARGV[1] then
    return 0
end

redis.call('DEL', KEYS[2])
if #ARGV >= 4 then
    local members = {}
    for i = 4, #ARGV do
        members[#members + 1] = ARGV[i]
    end
    if #members > 0 then
        redis.call('SADD', KEYS[2], unpack(members))
    end
end

local ttl = tonumber(ARGV[3])
redis.call('SET', KEYS[1], ARGV[2])
if ttl and ttl > 0 then
    redis.call('EXPIRE', KEYS[1], ttl)
    if redis.call('EXISTS', KEYS[2]) == 1 then
        redis.call('EXPIRE', KEYS[2], ttl)
    end
end
return 1
