--[[
  选课原子脚本：在 Redis 中完成「查重 + 扣减余量 + 写入已选集合」三个动作
  KEYS[1] = elective:course:capacity:{courseId}   课程剩余容量（String）
  KEYS[2] = elective:course:selected:{courseId}   已选学生集合（Set，成员为 studentId）
  ARGV[1] = studentId
  返回值：
    0 选课预占成功
    1 已选过该课程（重复选课）
    2 余量不足（课程已满）
    3 缓存未初始化（需要先执行预热/回源）
]]
local capacity = redis.call('GET', KEYS[1])
if capacity == false then
    return 3
end

if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then
    return 1
end

if tonumber(capacity) <= 0 then
    return 2
end

redis.call('DECR', KEYS[1])
redis.call('SADD', KEYS[2], ARGV[1])
return 0
