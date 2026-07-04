import { helpContent } from '../../lib/helpContent';
import type { HelpContentKey } from '../../lib/helpContent';

interface HelpTooltipProps {
  contentKey: HelpContentKey;
  compact?: boolean;
}

export function HelpTooltip({ contentKey, compact = false }: HelpTooltipProps) {
  const content = helpContent[contentKey];

  return (
    <span className="help-tooltip-wrap">
      <button
        type="button"
        className={compact ? 'help-icon help-icon-compact' : 'help-icon'}
        aria-label={`${content.title} 도움말`}
      >
        ?
      </button>
      <span className="help-tooltip" role="tooltip">
        <strong>{content.title}</strong>
        <span>{content.description}</span>
        {content.formula ? <em>{content.formula}</em> : null}
        {content.tip ? <span>{content.tip}</span> : null}
        {content.caution ? <span className="help-caution">{content.caution}</span> : null}
        {content.source ? <span>{content.source}</span> : null}
      </span>
    </span>
  );
}
