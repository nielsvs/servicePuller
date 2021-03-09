import styled from "styled-components";
import Button from "components/Button/styled";

export const ButtonGroup = styled.div`
  display: flex;
  justify-content: center;
  margin-top: 20px;
`;

export const AddButton = styled(Button)`
  background-color: #2a9d8f;
  margin-right: 10px;
`;

export const CancelButton = styled(Button)`
  background-color: #e9c46a;
`;

export const InputContainer = styled.div`
  display: flex;
  flex-direction: column;
`;

export const Form = styled.form`
  ${InputContainer}:first-child {
    margin-bottom: 10px;
  }
`;

export const Body = styled.div`
  width: 100%;
`;
