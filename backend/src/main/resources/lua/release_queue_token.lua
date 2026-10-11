--[[
  放弃排队 / 选课结束后清理令牌
  KEYS[1] = elective:selection:queue:{courseId}              排队队列（ZSET）
  KEYS[2] = elective:selection:token:{courseId}:{studentId}   该学生令牌（String）
  ARGV[1] = studentId
  返回值：
    1  确实清理了令牌
    0  本无令牌（幂等，重复调用不报错）
]]
redis.call('ZREM', KEYS[1], ARGV[1])
return redis.call('DEL', KEYS[2])