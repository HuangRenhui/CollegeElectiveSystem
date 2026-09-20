--[[
  退课原子脚本：从已选集合中移除学生，并归还余量
  KEYS[1] = elective:course:capacity:{courseId}   课程剩余容量（String）
  KEYS[2] = elective:course:selected:{courseId}   已选学生集合（Set）
  ARGV[1] = studentId
  ARGV[2] = ttlSeconds  缓存过期时间（秒），用于让两个 key 保持同一生命周期
  返回值：
    0 退课预占成功
    1 该学生未选此课程
]]
local ttl = tonumber(ARGV[2])

if redis.call('SREM', KEYS[2], ARGV[1]) == 1 then
    if redis.call('EXISTS', KEYS[1]) == 1 then
        redis.call('INCR', KEYS[1])
    end
    -- 续期，保持与容量 key 同一生命周期，避免集合残留而容量已回源
    if ttl and ttl > 0 then
        if redis.call('EXISTS', KEYS[1]) == 1 then
            redis.call('EXPIRE', KEYS[1], ttl)
        end
        if redis.call('EXISTS', KEYS[2]) == 1 then
            redis.call('EXPIRE', KEYS[2], ttl)
        end
    end
    return 0
end
return 1
