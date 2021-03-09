import React, { useEffect, useState } from "react";
import axios from "axios";
import { formatDistance } from "date-fns";
import { compareServices } from "./utilities";
import {
  AddServiceButton,
  Border,
  Container,
  deleteIcon,
  Description,
  editIcon,
  ServicesTable,
  ServicesTableActionCell,
  ServicesTableBody,
  ServicesTableHead,
  ServicesTableStatusCell,
  Title,
  Loading,
  PlaceholderContainer,
  PlaceholderText,
  PlaceholderImage,
} from "./styled";
import Confirm from "./components/Confirm";
import Edit from "./components/Edit";
import Add from "./components/Add";
import EmptyImage from "assets/images/empty.png";
import ErrorImage from "assets/images/error.png";
import { Service } from "types/service";
import TEXTS from "texts";
import { apiHost } from "config";

const {
  overview: {
    editErrorLabel,
    addErrorLabel,
    titleLabel,
    descriptionLabel,
    addServiceLabel,
    loadingErrorLabel,
    noServicesLabel,
  },
  general: {
    loadingLabel,
    urlLabel,
    nameLabel,
    updatedLabel,
    statusLabel,
    actionLabel,
  },
} = TEXTS;

/**
 * Overview of all services. Functionality for adding, updating and deleting individual services.
 */
export default function Overview() {
  const [services, setServices] = useState<Service[]>();
  const [error, setError] = useState<string>();
  const [deleteService, setDeleteService] = useState<Partial<Service>>();
  const [editService, setEditService] = useState<Partial<Service>>();
  const [addService, setAddService] = useState<boolean>(false);
  const [loading, setLoading] = useState<boolean>(true);

  // Fetch all services
  useEffect(() => {
    const fetchServices = async () => {
      try {
        const result = await axios.get(apiHost);
        setServices(result.data.services);
      } catch (err) {}
      setLoading(false);
    };
    fetchServices();
  }, []);

  // Delete service
  const deleteSelectedService = async (shouldDelete: boolean) => {
    if (deleteService && shouldDelete && services) {
      try {
        await axios.delete(apiHost, {
          data: { id: deleteService.Id },
        });
        const newServices = services.filter(
          (service) => service.Id !== deleteService.Id
        );
        setServices(newServices);
        setDeleteService(undefined);
      } catch (err) {
        setError(err);
      }
    } else {
      setDeleteService(undefined);
      setError(undefined);
    }
  };

  // Edit service
  const editSelectedService = async (service?: Partial<Service>) => {
    if (editService && service && services) {
      try {
        await axios.put(apiHost, {
          id: editService.Id,
          name: service.Name,
          service: service.Url,
        });
        // Update edited service
        const oldService = services.find(
          (service) => service.Id === editService.Id
        );

        if (!oldService) {
          setError(editErrorLabel);
        } else {
          const newService: Service = {
            ...oldService,
            Name: service.Name || "",
            Url: service.Url || "",
          };
          const newServices: Service[] = [
            ...services.filter((service) => service.Id !== editService.Id),
            newService,
          ].sort(compareServices);
          setServices(newServices);
          setEditService(undefined);
        }
      } catch (err) {
        setError(editErrorLabel);
      }
    } else {
      setEditService(undefined);
      setError(undefined);
    }
  };

  // Add service
  const addNewService = async (service?: Partial<Service>) => {
    if (service) {
      try {
        await axios.post(apiHost, {
          name: service.Name,
          service: service.Url,
        });

        // Fetch any new data
        const allServicesResult = await axios.get(apiHost);
        setServices(allServicesResult.data.services);

        setAddService(false);
      } catch (err) {
        setError(addErrorLabel);
      }
    } else {
      setAddService(false);
      setError(undefined);
    }
  };

  return (
    <Container>
      <Title>{titleLabel}</Title>
      <Description>{descriptionLabel}</Description>
      <AddServiceButton onClick={() => setAddService(true)}>
        {addServiceLabel}
      </AddServiceButton>
      {loading && (
        <Loading>
          <i className="fas fa-spin fa-spinner fa-2x"></i>
          <p>{loadingLabel}</p>
        </Loading>
      )}
      {!services && !loading && (
        <PlaceholderContainer>
          <PlaceholderImage src={ErrorImage} />
          <PlaceholderText>{loadingErrorLabel}</PlaceholderText>
        </PlaceholderContainer>
      )}
      {!loading && services && services.length === 0 && (
        <PlaceholderContainer>
          <PlaceholderImage src={EmptyImage} />
          <PlaceholderText>{noServicesLabel}</PlaceholderText>
        </PlaceholderContainer>
      )}
      {!loading && services && services.length > 0 && (
        <Border>
          <ServicesTable>
            <ServicesTableHead>
              <tr>
                <td>{urlLabel}</td>
                <td>{nameLabel}</td>
                <td>{statusLabel}</td>
                <td>{updatedLabel}</td>
                <td>{actionLabel}</td>
              </tr>
            </ServicesTableHead>
            <ServicesTableBody>
              {services.map(({ Id, Url, Name, Status, LastUpdated }) => (
                <tr key={Id}>
                  <td>{Url}</td>
                  <td>{Name}</td>
                  <ServicesTableStatusCell OK={Status === "OK"}>
                    {Status}
                  </ServicesTableStatusCell>
                  <td>
                    {formatDistance(Date.parse(LastUpdated), new Date(), {
                      includeSeconds: true,
                      addSuffix: true,
                    })}
                  </td>
                  <ServicesTableActionCell>
                    <i
                      className="fas fa-trash"
                      onClick={() => setDeleteService({ Id, Url, Name })}
                      style={deleteIcon}
                    ></i>
                    <i
                      onClick={() => setEditService({ Id, Url, Name })}
                      className="fas fa-edit"
                      style={editIcon}
                    ></i>
                  </ServicesTableActionCell>
                </tr>
              ))}
            </ServicesTableBody>
          </ServicesTable>
        </Border>
      )}
      {deleteService && (
        <Confirm
          name={deleteService.Name}
          url={deleteService.Url}
          savingError={error}
          shouldDelete={(shouldDelete: boolean) =>
            deleteSelectedService(shouldDelete)
          }
        />
      )}
      {editService && (
        <Edit
          name={editService.Name}
          url={editService.Url}
          savingError={error}
          edit={(service?: Partial<Service>) => editSelectedService(service)}
        />
      )}
      {addService && (
        <Add
          add={(service?: Partial<Service>) => addNewService(service)}
          savingError={error}
        />
      )}
    </Container>
  );
}
