import { HelpContentKey, helpContent } from '../../lib/helpContent';

interface HelpTextProps {
  contentKey: HelpContentKey;
}

export function HelpText({ contentKey }: HelpTextProps) {
  const content = helpContent[contentKey];

  return (
    <p className="help-text">
      <strong>{content.title}</strong>
      <span>{content.description}</span>
    </p>
  );
}
