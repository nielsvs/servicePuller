import styled from "styled-components";
import Button from "components/Button/styled";

export const ButtonGroup = styled.div`
  display: flex;
  margin-top: 20px;
`;

export const DeleteButton = styled(Button)`
  background-color: #e76f51;
  margin-right: 10px;
`;

export const CancelButton = styled(Button)`
  background-color: #e9c46a;
`;
