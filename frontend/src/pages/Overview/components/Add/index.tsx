import React, { useState } from "react";
import Modal from "components/Modal";
import { Input, InputLabel, InputError } from "components/Input/styled";
import GeneralError from "components/Error/styled";
import { Service } from "types/service";
import TEXTS from "texts";
import {
  AddButton,
  ButtonGroup,
  CancelButton,
  InputContainer,
  Form,
  Body,
} from "./styled";

const {
  addModal: {
    titleLabel,
    descriptionLabel,

    nameInputPlaceholderLabel,
    urlInputPlaceholderLabel,
  },
  general: {
    urlLabel,
    nameLabel,
    cancelLabel,
    savingLabel,
    saveLabel,
    missingUrlErrorLabel,
    invalidUrlErrorLabel,
  },
} = TEXTS;

interface Properties {
  add: (service?: Partial<Service>) => void;
  savingError?: string;
}

/**
 * Add service with name and url
 * @param add - callback function to add service
 * @param savingError - possible error when trying to add service
 */
export default function Add({ add, savingError }: Properties) {
  const [newName, setNewName] = useState<string>("");
  const [newUrl, setNewUrl] = useState<string>("");
  const [error, setError] = useState<string>();
  const [saving, setSaving] = useState<boolean>(false);

  // Validate input and invoke callback to save service
  const save = () => {
    setSaving(true);
    if (!newUrl || newUrl.length === 0) {
      setError(missingUrlErrorLabel);
      setSaving(false);
      return;
    }
    // Validate url
    try {
      // eslint-disable-next-line no-new
      new URL(newUrl);
    } catch {
      setError(invalidUrlErrorLabel);
      setSaving(false);
      return;
    }
    add({ Name: newName, Url: newUrl });
  };

  return (
    <Modal title={titleLabel} description={descriptionLabel}>
      <Body>
        <Form>
          <InputContainer>
            <InputLabel>{nameLabel}</InputLabel>
            <Input
              onChange={(event) => {
                setNewName(event.target.value);
              }}
              value={newName}
              placeholder={nameInputPlaceholderLabel}
            />
          </InputContainer>
          <InputContainer>
            <InputLabel>{urlLabel}</InputLabel>
            <Input
              onChange={(event) => {
                setNewUrl(event.target.value);
              }}
              value={newUrl}
              placeholder={urlInputPlaceholderLabel}
            />
            {error && <InputError>{error}</InputError>}
          </InputContainer>
        </Form>
        {savingError && <GeneralError>{savingError}</GeneralError>}
        <ButtonGroup>
          <AddButton disabled={saving} onClick={() => save()}>
            {saving ? savingLabel : saveLabel}
          </AddButton>
          <CancelButton onClick={() => add()}>{cancelLabel}</CancelButton>
        </ButtonGroup>
      </Body>
    </Modal>
  );
}
