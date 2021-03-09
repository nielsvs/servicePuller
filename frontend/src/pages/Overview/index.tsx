import React, { useEffect, useState } from "react";
import axios from "axios";
import { formatDistance } from "date-fns";
import EmptyImage from "assets/images/empty.png";
import ErrorImage from "assets/images/error.png";
import { Service } from "types/service";
import TEXTS from "texts";
import apiHost from "config";
import compareServices from "./utilities";
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

const {
  overview: {
    editErrorLabel,
    addErrorLabel,
    titleLabel,
    descriptionLabel,
    addServiceLabel,
    loadingErrorLabel,
    noServicesLabel,
    removeErrorLabel,
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
  const [error, setError] = useState<string | void>();
  const [deleteService, setDeleteService] = useState<Partial<Service> | void>();
  const [editService, setEditService] = useState<Partial<Service> | void>();
  const [addService, setAddService] = useState<boolean>(false);
  const [loading, setLoading] = useState<boolean>(true);

  // Fetch all services
  useEffect(() => {
    const fetchServices = async () => {
      try {
        const result = await axios.get(apiHost);
        setServices(result.data.services);
        setLoading(false);
      } catch {
        setLoading(false);
      }
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
        setDeleteService();
      } catch {
        setError(removeErrorLabel);
      }
    } else {
      setDeleteService();
      setError();
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
          (serviceElement) => serviceElement.Id === editService.Id
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
            ...services.filter(
              (serviceElement) => serviceElement.Id !== editService.Id
            ),
            newService,
          ].sort(compareServices);
          setServices(newServices);
          setEditService();
        }
      } catch {
        setError(editErrorLabel);
      }
    } else {
      setEditService();
      setError();
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
      } catch {
        setError(addErrorLabel);
      }
    } else {
      setAddService(false);
      setError();
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
                    {/* eslint-disable-next-line jsx-a11y/click-events-have-key-events, jsx-a11y/no-static-element-interactions */}
                    <i
                      className="fas fa-trash"
                      onClick={() => setDeleteService({ Id, Url, Name })}
                      style={deleteIcon}
                    ></i>
                    {/* eslint-disable-next-line jsx-a11y/click-events-have-key-events, jsx-a11y/no-static-element-interactions */}
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
          savingError={error || undefined}
          shouldDelete={(shouldDelete: boolean) =>
            deleteSelectedService(shouldDelete)
          }
        />
      )}
      {editService && (
        <Edit
          name={editService.Name}
          url={editService.Url}
          savingError={error || undefined}
          edit={(service?: Partial<Service>) => editSelectedService(service)}
        />
      )}
      {addService && (
        <Add
          add={(service?: Partial<Service>) => addNewService(service)}
          savingError={error || undefined}
        />
      )}
    </Container>
  );
}
