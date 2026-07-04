import { HelpContentKey, helpContent } from '../../lib/helpContent';
import { HelpTooltip } from './HelpTooltip';

interface HelpPopoverProps {
  contentKey: HelpContentKey;
}

export function HelpPopover({ contentKey }: HelpPopoverProps) {
  const content = helpContent[contentKey];

  return (
    <div className="help-popover-card">
      <div className="help-popover-title">
        <span>{content.title}</span>
        <HelpTooltip contentKey={contentKey} compact />
      </div>
      <p>{content.description}</p>
      {content.formula ? <p className="help-popover-meta">{content.formula}</p> : null}
      {content.tip ? <p className="help-popover-meta">{content.tip}</p> : null}
      {content.caution ? <p className="help-popover-caution">{content.caution}</p> : null}
    </div>
  );
}
