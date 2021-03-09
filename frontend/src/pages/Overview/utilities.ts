import { Service } from "types/service";

/**
 * Compare services based on the id (ASC)
 * @param a - service
 * @param b - service
 * @returns if the id of a is larger than b, equal or smaller (number)
 */
export default function compareServices(a: Service, b: Service) {
  if (a.Id < b.Id) {
    return -1;
  }
  if (a.Id > b.Id) {
    return 1;
  }
  return 0;
}
