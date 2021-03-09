import React from "react";
import axios from "axios";
import { render, screen, act } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import TEXT from "texts";
import Overview from ".";
import { Service } from "types/service";

jest.mock("axios");

const {
  overview: { noServicesLabel, loadingErrorLabel, addServiceLabel },
  addModal: { descriptionLabel },
} = TEXT;

// Mock data
const nameOfService1 = "Kry";
const statusOfService1 = "OK";
const nameOfService2 = "Livi";
const statusOfService2 = "FAIL";
const services: Service[] = [
  {
    Id: 1,
    Name: nameOfService1,
    Url: "https://kry.se",
    Status: statusOfService1,
    LastUpdated: "2021-03-08T20:59:41",
    Created: "2021-03-08T20:59:41",
  },
  {
    Id: 2,
    Name: nameOfService2,
    Url: "https://livis.fr",
    Status: statusOfService2,
    LastUpdated: "2021-03-08T20:59:41",
    Created: "2021-03-08T20:59:41",
  },
];

describe("Overview", () => {
  it("Should show empty placeholder, when no services added yet", async () => {
    // Have to make a promise in each test and solve it at the end to avoid the act() warning
    // https://kentcdodds.com/blog/fix-the-not-wrapped-in-act-warning
    const promise = Promise.resolve();

    // Mock return value from api
    (axios.get as jest.Mock).mockResolvedValue({
      data: { services: [] },
    });

    render(<Overview />);

    // Find empty placeholder text
    const emptyPlaceholderText = await screen.findByText(noServicesLabel);

    expect(emptyPlaceholderText).toBeDefined();

    // eslint-disable-next-line no-return-await
    await act(async () => await promise);
  });
  it("Should show error placeholder, when an error occurred while fetching initial data", async () => {
    const promise = Promise.resolve();

    // Mock error response from api
    (axios.get as jest.Mock).mockImplementation(() =>
      Promise.reject(new Error("Network Error"))
    );

    render(<Overview />);

    // Find error placeholder text
    const errorPlaceholder = await screen.findByText(loadingErrorLabel);

    expect(errorPlaceholder).toBeDefined();

    // eslint-disable-next-line no-return-await
    await act(async () => await promise);
  });
  it("Should show services, when data was fetched correctly", async () => {
    const promise = Promise.resolve();

    // Mock return value from api
    (axios.get as jest.Mock).mockResolvedValue({
      data: { services },
    });

    render(<Overview />);

    // Find name and status of first service
    const service1Name = await screen.findByText(nameOfService1);
    const service1Status = await screen.findByText(statusOfService1);

    // Find name and status of second service
    const service2Name = await screen.findByText(nameOfService2);
    const service2Status = await screen.findByText(statusOfService2);

    expect(service1Name).toBeDefined();
    expect(service1Status).toBeDefined();
    expect(service2Name).toBeDefined();
    expect(service2Status).toBeDefined();

    // eslint-disable-next-line no-return-await
    await act(async () => await promise);
  });
  it("Should show add service modal, when pressing add service button", async () => {
    const promise = Promise.resolve();

    // Mock return value from api
    (axios.get as jest.Mock).mockResolvedValue({
      data: { services },
    });

    render(<Overview />);

    // Find add service button
    const addServiceButton = await screen.findByText(addServiceLabel);

    // Click button
    userEvent.click(addServiceButton);

    // Find add service modal description
    const addServiceModalDescription = await screen.findByText(
      descriptionLabel
    );

    expect(addServiceModalDescription).toBeDefined();

    // eslint-disable-next-line no-return-await
    await act(async () => await promise);
  });
});
