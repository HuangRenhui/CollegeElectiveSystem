--[[
  选课排队取号：原子完成「重复取号判定 + 队列容量控制 + 发号 + 写入令牌」
  KEYS[1] = elective:selection:qstart:{courseId}              排队起始时刻（String）
  KEYS[2] = elective:selection:seq:{courseId}                 排队号发号器（String）
  KEYS[3] = elective:selection:queue:{courseId}               排队队列（ZSET，member=studentId）
  KEYS[4] = elective:selection:token:{courseId}:{studentId}   该学生令牌（String）
  ARGV[1] = studentId
  ARGV[2] = maxQueueSize     最大排队人数
  ARGV[3] = tokenTtlSeconds  令牌有效期（秒）
  ARGV[4] = nowMillis        当前服务器时刻（epoch 毫秒）
  返回值：
    -1  队列已满
    0   异常（令牌写入失败）
    >0  排队号（已有令牌时返回原号，保证幂等）
]]
local studentId = ARGV[1]
local maxQueueSize = tonumber(ARGV[2])
local tokenTtl = tonumber(ARGV[3])
local now = tonumber(ARGV[4])

-- 幂等：已持有令牌则直接返回原排队号。
-- 必须前置判断，否则学生刷新页面重复取号会拿到多个号、破坏先到先得
local existing = redis.call('GET', KEYS[4])
if existing then
    return tonumber(existing)
end

-- 队列容量控制：已达上限则拒绝，防止选课开放期间无限堆积占满内存。
-- 注意此处统计的是「在等的人数」，已叫号但尚未发起请求的令牌不在此计数内
local queued = redis.call('ZCARD', KEYS[3])
if queued >= maxQueueSize then
    return -1
end

-- 首次入队时记录起始时刻，作为后续「叫号」的时间基准。
-- TTL 取令牌有效期的 10 倍，保证整个排队周期内基准不失效
if redis.call('GET', KEYS[1]) == false then
    redis.call('SET', KEYS[1], now, 'EX', tokenTtl * 10)
end

-- 发号：INCR 保证同一课程下排队号严格递增，天然满足 FIFO 公平性，
-- 不会因后续请求「插队」而饿死先到者
local seq = redis.call('INCR', KEYS[2])

-- 写入令牌（带 TTL）。学生超时未完成选课时令牌自动失效，
-- 避免「取号后长期占位」阻塞后面的同学
redis.call('SET', KEYS[4], seq, 'EX', tokenTtl)

-- 记录队列成员用于统计排队人数；score 用排队号，便于按号取人与排查
redis.call('ZADD', KEYS[3], seq, studentId)
redis.call('EXPIRE', KEYS[3], tokenTtl * 10)
redis.call('EXPIRE', KEYS[2], tokenTtl * 10)

return seq