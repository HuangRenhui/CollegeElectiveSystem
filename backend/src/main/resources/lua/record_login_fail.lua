--[[
  登录失败计数原子脚本：在同一次 Redis 执行中完成计数累加与过期时间设置

  KEYS[1] = elective:login:fail:{username}   登录失败计数键
  ARGV[1] = 锁定窗口时长（秒）
  返回值  = 累加后的失败次数

  设计说明：
    1. INCR 与 EXPIRE 若拆分为两条命令，一旦在两者之间发生连接中断，
       Key 会已创建却未设置 TTL，导致该账号被永久锁定。合并为脚本可规避该风险。
    2. 仅在首次失败（计数为 1）时设置过期时间，后续失败不续期，
       防止攻击者通过慢速重试无限延长尝试窗口。
]]
local count = redis.call('INCR', KEYS[1])
if count == 1 then
    redis.call('EXPIRE', KEYS[1], ARGV[1])
end
return count
