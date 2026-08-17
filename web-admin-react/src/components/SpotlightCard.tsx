import { useRef, type ReactNode, type MouseEvent, type CSSProperties } from 'react'
import { cn } from '@/lib/utils'

interface SpotlightCardProps {
  children: ReactNode
  className?: string
  spotlightColor?: string
  spotlightSize?: number
  border?: boolean
  as?: 'div' | 'button'
  onClick?: () => void
  style?: CSSProperties
}

export function SpotlightCard({
  children,
  className,
  spotlightColor = 'rgba(139, 92, 246, 0.18)',
  spotlightSize = 280,
  border = true,
  as: Component = 'div',
  onClick,
  style,
}: SpotlightCardProps) {
  const cardRef = useRef<HTMLDivElement | HTMLButtonElement>(null)

  const handleMouseMove = (e: MouseEvent<HTMLDivElement | HTMLButtonElement>) => {
    const el = cardRef.current
    if (!el) return
    const rect = el.getBoundingClientRect()
    const x = e.clientX - rect.left
    const y = e.clientY - rect.top
    el.style.setProperty('--spotlight-x', `${x - spotlightSize / 2}px`)
    el.style.setProperty('--spotlight-y', `${y - spotlightSize / 2}px`)
  }

  return (
    <Component
      ref={cardRef as any}
      onMouseMove={handleMouseMove}
      onClick={onClick}
      className={cn(
        'group relative overflow-hidden rounded-xl bg-white transition-all duration-300',
        border && 'border border-neutral-100',
        'hover:border-primary-200 hover:shadow-md',
        onClick && 'cursor-pointer',
        className
      )}
      style={style}
    >
      {/* 聚光灯光斑 */}
      <div
        className="pointer-events-none absolute -inset-px z-0 opacity-0 transition-opacity duration-500 group-hover:opacity-100"
        style={{
          background: `radial-gradient(${spotlightSize}px circle at var(--spotlight-x) var(--spotlight-y), ${spotlightColor}, transparent 60%)`,
        }}
      />
      {/* 内容层 */}
      <div className="relative z-10 h-full">{children}</div>
    </Component>
  )
}
