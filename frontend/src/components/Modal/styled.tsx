import breakpoints from "style/breakpoints";
import styled from "styled-components";

export const Container = styled.div`
  display: block;
  position: fixed;
  z-index: 1;
  left: 0;
  top: 0;
  width: 100%;
  height: 100%;
  overflow: auto;
  background-color: rgb(0, 0, 0);
  background-color: rgba(0, 0, 0, 0.4);
`;

export const Content = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  background-color: #fefefe;
  margin: 15% auto;
  padding: 20px 80px;
  border: 1px solid #888;
  max-width: 300px;
  border-radius: 8px;

  @media (max-width: ${breakpoints.md}px) {
    padding: 20px;
  }
`;

export const Title = styled.h2``;

export const Description = styled.p``;
