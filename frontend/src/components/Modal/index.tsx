import React, { ReactNode } from "react";
import { Container, Content, Description, Title } from "./styled";

interface Properties {
  title: string;
  description: string;
  children?: ReactNode;
}

/**
 * Modal component with title, description and dynamic content
 * @param title - title for modal
 * @param description - description for modal
 * @param children - react children
 * @returns
 */
export default function Modal({ title, description, children }: Properties) {
  return (
    <Container>
      <Content>
        <Title>{title}</Title>
        <Description>{description}</Description>
        {children}
      </Content>
    </Container>
  );
}
