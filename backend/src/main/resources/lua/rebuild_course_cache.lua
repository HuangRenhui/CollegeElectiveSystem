--[[
  原子重建课程容量与已选集合。持锁期间 select/drop 脚本会拒绝执行。
  KEYS[1] = capacity key
  KEYS[2] = selected set key
  KEYS[3] = rebuild lock key
  KEYS[4] = 重建用的临时集合 key（写完 RENAME 覆盖 KEYS[2]）
  ARGV[1] = lock token
  ARGV[2] = remaining capacity
  ARGV[3] = ttl seconds
  ARGV[4..] = student ids（并集：数据库有效选课 ∪ Redis 中尚未落库的预占）
  返回：1 成功 / 0 非锁持有者
]]
if redis.call('GET', KEYS[3]) ~= ARGV[1] then
    return 0
end

local tmpKey = KEYS[4]
local ttl = tonumber(ARGV[3])

-- 先清空临时 key，保证重建从干净状态开始
redis.call('DEL', tmpKey)

--[[
  分批写入：Lua 5.1 的 unpack 受栈大小限制（约 8000 个元素），
  一次性 SADD 上千甚至上万成员会直接报 "too many results to unpack"，
  导致整段重建失败（结果就是容量 key 迟迟不写、选课持续返回 3010）。
  这里按 200 一批循环写入，既规避栈限制，也避免单条命令过大阻塞 Redis。
]]
local BATCH_SIZE = 200
local index = 4
while index <= #ARGV do
    local batch = {}
    local count = 0
    while count < BATCH_SIZE and index <= #ARGV do
        count = count + 1
        batch[count] = ARGV[index]
        index = index + 1
    end
    redis.call('SADD', tmpKey, unpack(batch))
end

--[[
  用 RENAME 原子替换，而不是先 DEL 再逐批 SADD：
  后者一旦中途失败，会留下「集合为空/残缺、容量 key 仍旧」的组合，
  造成已选学生被判为未选（重复选课）或余量错乱。
]]
if #ARGV >= 4 then
    redis.call('RENAME', tmpKey, KEYS[2])
else
    redis.call('DEL', KEYS[2])
end

redis.call('SET', KEYS[1], ARGV[2])
if ttl and ttl > 0 then
    redis.call('EXPIRE', KEYS[1], ttl)
    if redis.call('EXISTS', KEYS[2]) == 1 then
        redis.call('EXPIRE', KEYS[2], ttl)
    end
end
return 1
