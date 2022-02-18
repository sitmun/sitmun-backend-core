import {Table} from "react-bootstrap";
import React from "react";

export function Tasks({tasks}) {
  return (
    <Table>
      <thead>
      <tr className={"d-flex"}>
        <th className={"col-1"}>Id</th>
        <th className={"col-5"}>Name</th>
        <th className={"col-2"}>Type</th>
        <th className={"col-2"}>Group</th>
      </tr>
      </thead>
      <tbody>
      {tasks.sort((a, b) => a.id - b.id).map(task =>
        <tr key={task.id} className={"d-flex"}>
          <td className={"col-1"}><code>{task.id}</code></td>
          <td className={"col-5"}><code>{task.name}</code></td>
          <td className={"col-2"}><code>{task.type?.name}</code></td>
          <td className={"col-2"}><code>{task.group?.name}</code></td>
        </tr>
      )}
      </tbody>
    </Table>
  );

}