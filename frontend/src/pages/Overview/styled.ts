import Button from "components/Button/styled";
import breakpoints from "style/breakpoints";
import styled from "styled-components";

export const Container = styled.main`
  display: flex;
  align-items: center;
  flex-direction: column;
`;

export const Title = styled.h1``;

export const Description = styled.p``;

export const ServicesTable = styled.table`
  width: 100%;
  background-color: #264653;
  border-collapse: collapse;
`;

export const ServicesTableHead = styled.thead`
  tr {
    height: 50px;
    color: white;
    font-weight: bold;
  }
  td:first-child {
    padding-left: 10px;
  }
`;

export const ServicesTableBody = styled.tbody`
  background-color: white;
  tr {
    height: 40px;
  }
  tr:nth-child(even) {
    background: #e5e5e5;
  }
  td:first-child {
    padding-left: 10px;
  }

  tr:last-child {
    td:first-child {
      border-bottom-left-radius: 8px;
    }
    td:last-child {
      border-bottom-right-radius: 8px;
    }
  }
`;

interface ServicesTableStatusCellProperties {
  OK: boolean;
}

export const ServicesTableStatusCell = styled.td<ServicesTableStatusCellProperties>`
  ${({ OK }) => (OK ? "color: green;" : "color: red")}
`;

export const ServicesTableActionCell = styled.td`
  display: flex;
  align-items: center;
  height: 40px;
  padding: 0;
`;

export const Border = styled.div`
  width: 100%;
  border: 2px solid #264653;
  border-radius: 8px;
  overflow-x: scroll;
`;

export const AddServiceButton = styled(Button)`
  background-color: #2a9d8f;
  margin-bottom: 20px;
`;

export const Loading = styled.div`
  margin-top: 20px;
  text-align: center;
`;

export const PlaceholderContainer = styled.div`
  display: flex;
  flex-direction: column;
  justify-content: center;
`;

export const PlaceholderImage = styled.img`
  height: auto;
  width: 100%;
  max-width: 500px;
  align-self: center;
  @media (max-width: ${breakpoints.md}px) {
    max-height: 350px;
    width: auto;
  }
  @media (max-width: ${breakpoints.sm}px) {
    max-height: 200px;
    width: auto;
  }
`;

export const PlaceholderText = styled.h2`
  color: #264653;
  font-weight: lighter;
  text-align: center;
`;

// Icons styling
export const deleteIcon = {
  color: "red",
  marginRight: 10,
  cursor: "pointer",
};

export const editIcon = {
  cursor: "pointer",
};
