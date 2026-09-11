--[[
  退课原子脚本：从已选集合中移除学生，并归还余量
  KEYS[1] = elective:course:capacity:{courseId}   课程剩余容量（String）
  KEYS[2] = elective:course:selected:{courseId}   已选学生集合（Set）
  ARGV[1] = studentId
  返回值：
    0 退课预占成功
    1 该学生未选此课程
]]
if redis.call('SREM', KEYS[2], ARGV[1]) == 1 then
    if redis.call('EXISTS', KEYS[1]) == 1 then
        redis.call('INCR', KEYS[1])
    end
    return 0
end
return 1
